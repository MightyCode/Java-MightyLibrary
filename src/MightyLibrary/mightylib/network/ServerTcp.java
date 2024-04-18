package MightyLibrary.mightylib.network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ServerTcp {
    private final NetworkConfiguration configuration;
    private ServerSocket serverSocket;
    private Socket client;
    private BufferedReader in;
    private BufferedWriter out;
    public ServerTcp(NetworkConfiguration configuration){
        this.configuration = configuration;
    }

    private final static Charset ENCODING = StandardCharsets.UTF_8;

    public void tryCreateConnection(){
        System.out.println(configuration.getPort());
        try {
            serverSocket = new ServerSocket(configuration.getPort());
        } catch (IOException e) {
            serverSocket = null;
            e.printStackTrace();
            System.out.println("Can't create socket with port : " + configuration.getPort());
        }
    }

    public boolean hasClient() {
        return client != null;
    }

    public void acceptConnexion() {
        try {
            client = serverSocket.accept();
            out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
            in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitForConnection() {
        while (!hasClient()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeMessageLength(OutputStream stream, int length) throws IOException {
        byte[] lengthBytes = ByteBuffer.allocate(4).putInt(length).array();
        stream.write(lengthBytes);
    }

    public void sendMessage(String message) {
        if (client != null && out != null) {
            try {
                byte[] dataBytes = message.getBytes(StandardCharsets.UTF_8);
                int dataLength = dataBytes.length;
                writeMessageLength(client.getOutputStream(), dataLength);
                out.write(message);
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private int readMessageLength(InputStream stream) throws IOException {
        byte[] lengthBytes = new byte[4];
        int bytesRead = stream.read(lengthBytes);
        if (bytesRead != 4) {
            throw new IOException("Unable to read the message length.");
        }
        return ByteBuffer.wrap(lengthBytes).getInt();
    }


    public String readMessage() {
        if (client == null) {
            System.out.println("Can't read message while connection not initialized");
            return null;
        }

        String message = null;

        try {
            int messageLength = readMessageLength(client.getInputStream());
            char[] charMessage = new char[messageLength];
            int bytesRead = in.read(charMessage, 0, messageLength);

            if (bytesRead == messageLength) {
                message = new String(charMessage);
            } else {
                throw new IOException("Unable to read the complete message.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return message;
    }
    public void closeConnexion() {
        if (client == null) {
            System.out.println("Can't close uncreated connection.");
            return;
        }

        try {
            client.shutdownInput();
            client.shutdownOutput();
            client.close();
            serverSocket.close();
        } catch (IOException e) {
            client = null;
            System.out.println("Error while closing connection\n");
            e.printStackTrace();
        }
    }

}
