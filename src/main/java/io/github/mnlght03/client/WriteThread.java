package io.github.mnlght03.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class WriteThread extends Thread {
    private SocketChannel serverChannel;
    private BufferedReader stdInReader;

    public WriteThread(SocketChannel serverChannel) {
        this.serverChannel = serverChannel;
        this.stdInReader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void run() {
        String msg = "";

        while (true) {
            try {
                msg = stdInReader.readLine();
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                serverChannel.write(buffer);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }   // while
    }   // run
}
