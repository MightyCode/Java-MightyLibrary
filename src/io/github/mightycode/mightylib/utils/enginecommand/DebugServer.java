package io.github.mightycode.mightylib.utils.enginecommand;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Local server to send commands to the running program from another process (scripts, tools...).
 * It only listens on the loopback address, so it is not reachable from another computer.
 * Protocol : one command per line, one JSON line in answer : {"success": bool, "message": "...", "data": {...}}
 */
public class DebugServer {
    private static final long STOP_WAIT_MILLISECONDS = 500;
    private static final int MAX_PORT = 65535;

    // System property: file where the port really used is written once the server listens
    public static final String PORT_FILE_PROPERTY = "mightylib.debugServer.portFile";

    private final Commands commands;
    private final int port;

    private ServerSocket serverSocket;
    // Answers being written, waited at stop so the client of an exit command gets its answer
    private final AtomicInteger pendingAnswers;

    public DebugServer(Commands commands, int port) {
        this.commands = commands;
        this.port = port;
        this.pendingAnswers = new AtomicInteger();
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
        try (Socket socket = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                pendingAnswers.incrementAndGet();
                try {
                    CommandResult result;
                    try {
                        result = commands.submit(line).get(Commands.COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        result = CommandResult.error("No answer after " + Commands.COMMAND_TIMEOUT_SECONDS + " seconds");
                    } catch (ExecutionException e) {
                        result = CommandResult.error(e.getCause().toString());
                    }

                    out.write(result.toJson().toString());
                    out.write('\n');
                    out.flush();
                } finally {
                    pendingAnswers.decrementAndGet();
                }
            }
        } catch (IOException e) {
            // Client disconnected
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
