package io.github.mnlght03.client.socketClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class ChatClient {
    private static final int BUFFER_SIZE = 1024;

    private String username;
    private InetSocketAddress serverAddress;
    private SocketChannel socketChannel;
    private ByteBuffer buffer;

    public ChatClient(String host, int port) {
        this.serverAddress = new InetSocketAddress(host, port);
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    public void start() throws IOException {
        socketChannel = SocketChannel.open(serverAddress);
        System.out.println("Connected to chat server");

        while (true) {
            readFromServer();

            String message = readFromConsole();
            if (message != null && !message.isEmpty()) {
                sendMessage(message);
            }
        }
    }

    private void readFromServer() throws IOException {
        buffer.clear();
        int numBytes = socketChannel.read(buffer);
        if (numBytes == -1) {
            throw new IOException("Server has closed the connection");
        }

        buffer.flip();
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
        System.out.println(charBuffer.toString());
    }

    private String readFromConsole() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("> ");
        String input = reader.readLine();
        if (username == null)
            username = input;
        return input;
    }

    private void sendMessage(String message) throws IOException {
        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        socketChannel.write(buffer);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java ChatClient <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        new ChatClient(host, port).start();
    }
}