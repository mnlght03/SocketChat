package io.github.mnlght03.xmlchat.server;

import io.github.mnlght03.xmlchat.xmlhandler.XMLCommand;
import io.github.mnlght03.xmlchat.xmlhandler.XMLSerializer;

import javax.xml.bind.JAXBException;
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

public class XMLChatServer {
    private InetSocketAddress address;
    private Set<SocketChannel> userChannels = new HashSet<>();
    private Map<SocketChannel, String> usernames = new HashMap<>();
    private Selector selector;
    private int MAX_BUFFER_SIZE = 1024;


    public XMLChatServer(String hostname, int port) {
        this.address = new InetSocketAddress(hostname, port);
    }

    public int getMaxBufferSize() {
        return this.MAX_BUFFER_SIZE;
    }

    public void setMaxBufferSize(int size) {
        this.MAX_BUFFER_SIZE = size;
    }

    public void start() throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(address);
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();
                keys.remove();

                if (key.isAcceptable()) {
                    accept(key);
                }

                else if (key.isReadable()) {
                    read(key);
                }
            }   // while (keys.hasNext())

        }   // while (true)
    }   // start()

    private void accept(SelectionKey key) {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel userChannel = serverSocketChannel.accept();
            userChannel.configureBlocking(false);
            userChannels.add(userChannel);
            userChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void read(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
            int readBytes = channel.read(buffer);

            if (readBytes > 4) {
                buffer.flip();
                int readTotal = 0;
                int msgSize = buffer.getInt();
                StringBuilder sb = new StringBuilder();

                while (readTotal < msgSize && readBytes > 0) {
                    CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
                    sb.append(charBuffer.toString());
                    readTotal += readBytes;
                    buffer.flip();
                    readBytes = channel.read(buffer);
                    buffer.flip();
                }

                XMLCommand command = (XMLCommand) XMLSerializer.deserialize(new XMLCommand(), sb.toString());

                if (command.getName().equals("login")) login(channel, command.getName(), command.getType());

                else if (command.getName().equals("list")) listUsers(command.getSession());

                else if (command.getName().equals("message")) broadcast(channel, command.getMessage(), command.getSession());

                else if (command.getName().equals("logout")) logout(key, command.getSession());

                // else wrong command error

            }

            if (readBytes == -1) {
                notifyUserDisconnected(key);
            }
        } catch (SocketException ex) {
            notifyUserDisconnected(key);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (JAXBException ex) {
            // error respond
        }
    }   // read()

    private void login(SocketChannel channel, String username, String type) {

    }

//    private void notifyUserDisconnected(SelectionKey key) {
//        SocketChannel channel = (SocketChannel) key.channel();
//        broadcast(channel, "User " + usernames.get(channel) + "(" + channel.socket().getRemoteSocketAddress() + ") disconnected\n");
//        usernames.remove(channel);
//        userChannels.remove(channel);
//        try {
//            channel.close();
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//        key.cancel();
//    }   // notifyUserDisconnect()

    private void broadcast(SocketChannel sender, String msg, String session) {
        msg += "\n";
        ByteBuffer buffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
        buffer.put(msg.getBytes());
        buffer.flip();

        System.out.print("Broadcasting: " + msg);

        userChannels.forEach(userChannel -> {
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

}
