package io.github.mnlght03.xmlchat.server;

import io.github.mnlght03.xmlchat.xmlserializer.*;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private Map<SocketChannel, String> types = new HashMap<>();
    private Map<SocketChannel, String> sessions = new HashMap<>();
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

                if (msgSize > MAX_BUFFER_SIZE) {
                    XMLError errorMessage = XMLHandler.createErrorMessage(
                            "Message is too large. Max message size is "+ MAX_BUFFER_SIZE + " Bytes"
                    );

                    send(channel, XMLSerializer.serialize(errorMessage));
                }

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

                else if (command.getName().equals("list")) listUsers(channel, command.getSession());

                else if (command.getName().equals("message")) broadcastMessage(channel, command.getMessage(), command.getSession());

                else if (command.getName().equals("logout")) logout(key, command.getSession());

                else {
                    XMLError errorMessage = XMLHandler.createErrorMessage("Wrong command name");
                    send(channel, XMLSerializer.serialize(errorMessage));
                }
            }

            if (readBytes == -1) {
                channel.close();
                key.cancel();
            }
        } catch (SocketException ex) {
            try {
                key.channel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            key.cancel();
        } catch (UnmarshalException ex) {
            SocketChannel channel = (SocketChannel) key.channel();
            XMLError errorMessage = XMLHandler.createErrorMessage("Invalid xml root element");

            try {
                send(channel, XMLSerializer.serialize(errorMessage));
            } catch (IOException | JAXBException e) {
                e.printStackTrace();
            }
        } catch (IOException | JAXBException ex) {
            ex.printStackTrace();
        }
    }   // read()

    private void sendWrongSessionError(SocketChannel channel) {
        XMLError errorMessage = XMLHandler.createErrorMessage("Provided session is not yours");
        try {
            send(channel, XMLSerializer.serialize(errorMessage));
        } catch(IOException | JAXBException ex) {
            ex.printStackTrace();
        }
    }   // sendWrongSessionError()

    private void login(SocketChannel channel, String username, String type) {
        if (usernames.containsValue(username)) {
            XMLError errorMessage = XMLHandler.createErrorMessage("This username is already taken");

            try {
                send(channel, XMLSerializer.serialize(errorMessage));
            } catch (IOException | JAXBException ex) {
                ex.printStackTrace();
            }

            return;
        }

        usernames.put(channel, username);
        types.put(channel, type);
        sessions.put(channel, channel.socket().getRemoteSocketAddress().toString());
        try {
            XMLSuccess successMessage = XMLHandler.createSuccessMessage(sessions.get(channel), null);
            String xml = XMLSerializer.serialize(successMessage);
            send(channel, xml);

            XMLEvent eventMessage = XMLHandler.createEventMessage("userlogin", username, null);
            broadcast(channel, XMLSerializer.serialize(eventMessage));
        } catch (JAXBException | IOException ex) {
            ex.printStackTrace();
        }
    }   // login()

    private void listUsers(SocketChannel channel, String session) {
        if (sessions.get(channel) != session) {
            sendWrongSessionError(channel);
            return;
        }

        List<XMLUser> userList = new ArrayList<>();

        userChannels.forEach(userChannel -> {
            if (userChannel != channel) {
                XMLUser user = new XMLUser();
                user.setName(usernames.get(userChannel));
                user.setType(types.get(userChannel));
                userList.add(user);
            }
        });

        XMLSuccess successMessage = XMLHandler.createSuccessMessage(null, userList);

        try {
            send(channel, XMLSerializer.serialize(successMessage));
        } catch (IOException | JAXBException ex) {
            ex.printStackTrace();
        }
    }   // listUsers()

    private void broadcastMessage(SocketChannel channel, String message, String session) {
        if (sessions.get(channel) != session) {
            sendWrongSessionError(channel);
            return;
        }

        XMLEvent eventMessage = XMLHandler.createEventMessage("message", usernames.get(channel), message);

        try {
            broadcast(channel, XMLSerializer.serialize(eventMessage));
        } catch (IOException | JAXBException ex) {
            ex.printStackTrace();
        }
    }   // broadcastMessage()

    private void logout(SelectionKey key, String session) {
        SocketChannel channel = (SocketChannel) key.channel();

        if (sessions.get(channel) != session) {
            sendWrongSessionError(channel);
            return;
        }
        try {
            XMLSuccess successMessage = XMLHandler.createSuccessMessage(null, null);
            send(channel, XMLSerializer.serialize(successMessage));

            XMLEvent eventMessage = XMLHandler.createEventMessage("userlogout", usernames.get(channel), null);
            broadcast(channel, XMLSerializer.serialize(eventMessage));

            key.cancel();
            channel.close();
        } catch (IOException | JAXBException ex) {
            ex.printStackTrace();
        }
    }


    private void send(SocketChannel channel, String xml) {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
        byte[] bytes = xml.getBytes();
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        try {
            channel.write(buffer);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void broadcast(SocketChannel sender, String msg) {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
        buffer.put(msg.getBytes());
        buffer.flip();

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
