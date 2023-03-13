package io.github.mnlght03.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ReadThread extends Thread {
    private SocketChannel serverChannel;

    public ReadThread(SocketChannel serverChannel) {
        this.serverChannel = serverChannel;
    }

    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (true) {
            buffer.clear();
            try {
                int bytesRead = serverChannel.read(buffer);

                if (bytesRead == 0) {
                    continue;
                }

                else if (bytesRead == -1) {
                    System.out.println("Server closed connection");
                    break;
                }

                buffer.flip();

                CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
                String msg = charBuffer.toString();
                System.out.print(msg);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }   // while
    }   // run
}
