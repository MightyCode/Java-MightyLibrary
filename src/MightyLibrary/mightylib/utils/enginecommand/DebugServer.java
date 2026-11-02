package MightyLibrary.mightylib.utils.enginecommand;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
    private static final long COMMAND_TIMEOUT_SECONDS = 30;
    private static final long STOP_WAIT_MILLISECONDS = 500;

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

    public boolean start() {
        try {
            serverSocket = new ServerSocket(port, 4, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            System.err.println("Debug server can't listen on port " + port + " : " + e);
            return false;
        }

        Thread acceptThread = new Thread(this::acceptClients, "MightyLibrary-DebugServer");
        acceptThread.setDaemon(true);
        acceptThread.start();

        System.out.println("--Debug server listening on " + serverSocket.getInetAddress().getHostAddress() + ":" + port);

        return true;
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
                        result = commands.submit(line).get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        result = CommandResult.error("No answer after " + COMMAND_TIMEOUT_SECONDS + " seconds");
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
