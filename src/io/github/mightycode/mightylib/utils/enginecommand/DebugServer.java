package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.console.ConsoleLog;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Local server to send commands to the running program from another process (scripts, tools...).
 * It only listens on the loopback address, so it is not reachable from another computer.
 * Protocol : one command per line, one JSON line in answer : {"success": bool, "message": "...", "data": {...}}
 * A client following the console log ("console follow on") also receives, between the answers, one line per log line :
 * {"event": "log", "id": n, "type": "OUT|ERR|COMMAND|RESULT|ERROR", "source": "...", "text": "..."}
 */
public class DebugServer {
    private static final long STOP_WAIT_MILLISECONDS = 500;
    private static final int MAX_PORT = 65535;

    // System property: file where the port really used is written once the server listens
    public static final String PORT_FILE_PROPERTY = "mightylib.debugServer.portFile";

    private final Commands commands;
    private final int port;
    // Followed by the clients asking for it, null: no follow
    private final ConsoleLog log;
    private final AtomicInteger nextClient;

    private ServerSocket serverSocket;
    // Answers being written, waited at stop so the client of an exit command gets its answer
    private final AtomicInteger pendingAnswers;

    public DebugServer(Commands commands, int port) {
        this(commands, port, null);
    }

    public DebugServer(Commands commands, int port, ConsoleLog log) {
        this.commands = commands;
        this.port = port;
        this.log = log;
        this.pendingAnswers = new AtomicInteger();
        this.nextClient = new AtomicInteger(1);
    }

    /**
     * One connection: the source of its commands, its writer shared by the answers and the followed log lines.
     * Followed lines are queued and written by a thread of their own: a client that reads slowly never blocks the
     * thread writing to the log (often the main one); past FOLLOW_QUEUE_SIZE waiting lines, new ones are dropped.
     */
    private final class Client implements ICommandSource {
        private static final int FOLLOW_QUEUE_SIZE = 10000;

        private final String name;
        private final BufferedWriter out;
        private final Consumer<ConsoleLog.Line> follower;
        private final LinkedBlockingQueue<ConsoleLog.Line> followed;
        private Thread followThread;

        private Client(BufferedWriter out) {
            this.name = "server#" + nextClient.getAndIncrement();
            this.out = out;
            this.followed = new LinkedBlockingQueue<>(FOLLOW_QUEUE_SIZE);
            this.follower = followed::offer;
        }

        @Override
        public String name() {
            return name;
        }

        // The debug server only exists in debug mode
        @Override
        public ECommandLevel level() {
            return ECommandLevel.DEV;
        }

        @Override
        public synchronized boolean setFollowLog(boolean follow) {
            if (log == null)
                return false;

            if (follow && followThread == null) {
                followThread = new Thread(this::writeFollowed, "MightyLibrary-DebugFollow");
                followThread.setDaemon(true);
                followThread.start();
                log.addListener(follower);
            } else if (!follow && followThread != null) {
                log.removeListener(follower);
                followThread.interrupt();
                followThread = null;
                followed.clear();
            }

            return true;
        }

        private void writeFollowed() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ConsoleLog.Line line = followed.take();
                    send(new JSONObject()
                            .put("event", "log")
                            .put("id", line.getId())
                            .put("type", line.getType().name())
                            .put("source", line.getSource() == null ? JSONObject.NULL : line.getSource())
                            .put("text", line.getText()));
                }
            } catch (InterruptedException e) {
                // Follow stopped
                Thread.currentThread().interrupt();
            }
        }

        // From any thread: a failed write means the client is gone, its reading loop ends
        private void send(JSONObject json) {
            synchronized (out) {
                try {
                    out.write(json.toString());
                    out.write('\n');
                    out.flush();
                } catch (IOException e) {
                    // Disconnected
                }
            }
        }
    }

    /**
     * Listen on the requested port, or on the next free one (another game may already use it).
     * The port really used is printed, given by getPort(), and written in the file named by the
     * system property PORT_FILE_PROPERTY when it is set (used by tools/scenario.py).
     */
    public boolean start() {
        for (int candidate = port; candidate <= MAX_PORT; ++candidate) {
            try {
                serverSocket = new ServerSocket(candidate, 4, InetAddress.getLoopbackAddress());
                break;
            } catch (BindException e) {
                // Port already used: try the next one
            } catch (IOException e) {
                System.err.println("Debug server can't listen on port " + candidate + " : " + e);
                return false;
            }
        }

        if (serverSocket == null) {
            System.err.println("Debug server can't find a free port from " + port);
            return false;
        }

        Thread acceptThread = new Thread(this::acceptClients, "MightyLibrary-DebugServer");
        acceptThread.setDaemon(true);
        acceptThread.start();

        if (getPort() != port)
            System.out.println("--Debug server : port " + port + " already used");
        System.out.println("--Debug server listening on " + serverSocket.getInetAddress().getHostAddress() + ":" + getPort());

        writePortFile();

        return true;
    }

    // Port really listened, -1 if the server is not started
    public int getPort() {
        return serverSocket == null ? -1 : serverSocket.getLocalPort();
    }

    private void writePortFile() {
        String portFile = System.getProperty(PORT_FILE_PROPERTY);
        if (portFile == null || portFile.isEmpty())
            return;

        try {
            Files.write(Paths.get(portFile), String.valueOf(getPort()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Debug server can't write its port in " + portFile + " : " + e);
        }
    }

    private void acceptClients() {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();

                Thread clientThread = new Thread(() -> handleClient(client), "MightyLibrary-DebugClient");
                clientThread.setDaemon(true);
                clientThread.start();
            } catch (IOException e) {
                // Closed by stop()
                return;
            }
        }
    }

    private void handleClient(Socket client) {
        Client connection = null;
        try (Socket socket = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            connection = new Client(out);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                pendingAnswers.incrementAndGet();
                try {
                    CommandResult result;
                    try {
                        // Always completed once executed: deferred results have their own timeout
                        result = commands.submit(line, connection).get();
                    } catch (ExecutionException e) {
                        result = CommandResult.error(e.getCause().toString());
                    }

                    connection.send(result.toJson());
                } finally {
                    pendingAnswers.decrementAndGet();
                }
            }
        } catch (IOException e) {
            // Client disconnected
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (connection != null)
                connection.setFollowLog(false);
        }
    }

    public void stop() {
        if (serverSocket == null)
            return;

        // Let the answers being written reach their clients
        long end = System.currentTimeMillis() + STOP_WAIT_MILLISECONDS;
        while (pendingAnswers.get() > 0 && System.currentTimeMillis() < end)
            Thread.onSpinWait();

        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // Already closed
        }
    }
}
