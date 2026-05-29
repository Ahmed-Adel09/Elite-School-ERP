package com.elite.erp.network;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NotificationServer — demonstrates mandatory Socket Programming requirement.
 *
 * Runs a ServerSocket on port 9999 in a daemon thread.
 * Accepts multiple clients and broadcasts admission result notifications
 * to all connected clients.
 *
 * Usage:
 *   NotificationServer.start();      // call once on app startup
 *   NotificationServer.broadcast("Application #5 APPROVED");
 */
public class NotificationServer {

    private static final int PORT = 9999;
    private static ServerSocket serverSocket;
    private static final CopyOnWriteArrayList<PrintWriter> clients = new CopyOnWriteArrayList<>();
    private static boolean running = false;

    private NotificationServer() {}

    /** Start the server in a daemon thread so it stops with the JVM */
    public static void start() {
        if (running) return;
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                running = true;
                System.out.println("[Server] NotificationServer listening on port " + PORT);

                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (SocketException e) {
                        if (running) System.err.println("[Server] Socket error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("[Server] Failed to start: " + e.getMessage());
            }
        }, "NotificationServer-Thread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private static void handleClient(Socket socket) {
        Thread clientThread = new Thread(() -> {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true);
                clients.add(writer);
                writer.println("[Server] Connected. Listening for notifications...");

                // Keep connection alive — read any ping messages
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Server] Client says: " + line);
                }
            } catch (IOException e) {
                System.err.println("[Server] Client disconnected: " + e.getMessage());
            }
        }, "ClientHandler-" + socket.getPort());
        clientThread.setDaemon(true);
        clientThread.start();
    }

    /** Broadcast a message to all connected clients */
    public static void broadcast(String message) {
        String timestamped = "[" + LocalTime.now().withNano(0) + "] " + message;
        System.out.println("[Server] Broadcasting: " + timestamped);
        clients.removeIf(PrintWriter::checkError);
        clients.forEach(writer -> writer.println(timestamped));
    }

    public static void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) {}
    }

    public static boolean isRunning() { return running; }
}
