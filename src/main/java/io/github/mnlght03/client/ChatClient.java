package io.github.mnlght03.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class ChatClient {
    public static void main(String [] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java ChatClient <hostname> <port>");
            System.exit(0);
        }
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        SocketChannel serverChannel = SocketChannel.open(new InetSocketAddress(hostname, port));

        new ReadThread(serverChannel).start();
        new WriteThread(serverChannel).start();
    }   // main
}
