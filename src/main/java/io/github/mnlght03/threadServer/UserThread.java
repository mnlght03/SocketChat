package io.github.mnlght03.threadServer;


import java.io.*;
import java.net.*;

public class UserThread extends Thread {
    private Socket socket;
    private ThreadServer server;
    private PrintWriter writer;

    public UserThread(Socket socket, ThreadServer server) {
        this.server = server;
        this.socket = socket;
    }

    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            printUsers();

            String userName = reader.readLine();
            server.addUserName(userName);

            String serverMsg = "New user connected: " + userName;
            server.broadcast(serverMsg, this);

            String clientMsg = "";

            while (!clientMsg.equalsIgnoreCase("quit")) {
                clientMsg = reader.readLine();
                serverMsg = userName + ": " + clientMsg;
                server.broadcast(serverMsg, this);
            }

            server.removeUser(userName, this);
            socket.close();

            serverMsg = userName + " quitted";
            server.broadcast(serverMsg, this);
        } catch (IOException ex) {
            System.out.println("Error in UserThread: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void printUsers() {
        if (server.hasUsers()) {
            writer.println("Connected users: " + server.getUserNames());
        } else {
            writer.println("No users connected");
        }
    }

    void sendMessage(String msg) {
        writer.println(msg);
    }

}
