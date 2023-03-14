package io.github.mnlght03.nioserver;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.*;

public class ChatServer {
    private InetSocketAddress address;
    private Selector selector;
    private Set<SocketChannel> userSocketChannels;
    private Map<SocketChannel, String> usernames;
    private int MAX_USERNAME_LEN = 128;
    private int MAX_BUFFER_SIZE = 1024;

    public ChatServer(String hostname, int port) {
        this.address = new InetSocketAddress(hostname, port);
        this.userSocketChannels = new HashSet<SocketChannel>();
        this.usernames = new HashMap<SocketChannel, String>();
    }

    public void setMaxUsernameLen(int len) {
        this.MAX_USERNAME_LEN = len;
    }

    public int getMaxUsernameLen() {
        return this.MAX_USERNAME_LEN;
    }

    public void setMaxBufferSize(int size) {
        this.MAX_BUFFER_SIZE = size;
    }

    public int getMaxBufferSize() {
        return this.MAX_BUFFER_SIZE;
    }

    public void start() throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(address);
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server listening...");

        while (true) {
            selector.select();
            Iterator keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                }
            }   // while (keys.hasNext())
        }   // while (true)
    }   // start()

    // TODO: add exceeding username len handling
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        userSocketChannels.add(channel);

        ByteBuffer buffer = ByteBuffer.allocate(MAX_USERNAME_LEN);
        buffer.put("Enter username: ".getBytes());
        buffer.flip();
        channel.write(buffer);
    }   // accept()

    private void read(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
            int bytesRead;

            try {
                bytesRead = channel.read(buffer);
            } catch (SocketException ex) {
                notifyUserDisconnected(key);
                return;
            }

            if (bytesRead == -1) {
                notifyUserDisconnected(key);
                return;
            }

            buffer.flip();
            CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
            String msg = charBuffer.toString().trim();

            if (!usernames.containsKey(channel)) {
                usernames.put(channel, msg);
                System.out.println(channel.socket().getRemoteSocketAddress() + " username is: " + msg);
                broadcast(channel, "New user connected: " + msg);
            }

            else {
                broadcast(channel, usernames.get(channel) + ": " + msg);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }   // read()

    private void notifyUserDisconnected(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        broadcast(channel, "User " + usernames.get(channel) + "(" + channel.socket().getRemoteSocketAddress() + ") disconnected\n");
        usernames.remove(channel);
        userSocketChannels.remove(channel);
        try {
            channel.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        key.cancel();
    }   // notifyUserDisconnect()

    private void broadcast(SocketChannel sender, String msg) {
        msg += "\n";
        ByteBuffer buffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
        buffer.put(msg.getBytes());
        buffer.flip();

        System.out.print("Broadcasting: " + msg);

        userSocketChannels.forEach(userChannel -> {
            if (userChannel == sender) {
                return;
            }

            try {
                userChannel.write(buffer);
                buffer.flip();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }   // broadcast()

    public static void main(String[] args) throws IOException {
        new ChatServer("localhost", 3000).start();
    }

}