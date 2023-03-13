package io.github.mnlght03.threadServer;

import java.net.*;
import java.io.*;
import java.util.*;


public class ThreadServer {
    private int port;
    private Set<String> userNames = new HashSet<>();
    private Set<UserThread> userThreads = new HashSet<>();
    public ThreadServer(int port) {
        this.port = port;
    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat server is listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New user connected");

                UserThread newUser = new UserThread(clientSocket, this);
                userThreads.add(newUser);
                newUser.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java Server <port_number>");
            System.exit(0);
        }
        int port = Integer.parseInt(args[0]);
        ThreadServer server = new ThreadServer(port);
        server.execute();
    }

    void broadcast(String msg, UserThread sender) {
        for (UserThread user: userThreads) {
            if (user != sender)
                user.sendMessage(msg);
        }
    }

    void addUserName(String userName) {
        userNames.add(userName);
    }

    void removeUser(String userName, UserThread user) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            userThreads.remove(user);
            System.out.println("User " + userName + " disconnected");
        }
    }

    Set<String> getUserNames() {
        return this.userNames;
    }

    boolean hasUsers() {
        return !this.userNames.isEmpty();
    }

}

