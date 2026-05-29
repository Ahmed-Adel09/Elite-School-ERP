package com.elite.erp.network;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * LibraryServer — Handles Book Reservation requests and Overdue Broadcasts.
 * Listens on Port 9997.
 */
public class LibraryServer {

    private static final int PORT = 9997;
    private static ServerSocket serverSocket;
    private static final CopyOnWriteArrayList<PrintWriter> clients = new CopyOnWriteArrayList<>();
    private static boolean running = false;

    private LibraryServer() {}

    public static void start() {
        if (running) return;
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                running = true;
                System.out.println("[LibraryServer] Listening on port " + PORT);

                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (SocketException e) {
                        if (running) System.err.println("[LibraryServer] Socket error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("[LibraryServer] Failed to start: " + e.getMessage());
            }
        }, "LibraryServer-Thread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private static void handleClient(Socket socket) {
        Thread clientThread = new Thread(() -> {
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                clients.add(writer);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("RESERVE|")) {
                        // Forward reservation request to the Librarian
                        broadcast("[LIBRARIAN_ALERT] " + line.substring(8));
                    } else if (line.startsWith("OVERDUE|")) {
                        // Forward overdue warning
                        broadcast("[OVERDUE_WARNING] " + line.substring(8));
                    } else if (line.startsWith("EMERGENCY|")) {
                        // Student requesting nurse assistance — broadcast to all
                        broadcast("[🚨 EMERGENCY] " + line.substring(10));
                    } else if (line.startsWith("NURSE_REGISTER|")) {
                        // Nurse dashboard registered as a persistent listener — no broadcast needed
                        System.out.println("[LibraryServer] Nurse dashboard connected.");
                    }
                }
            } catch (Exception e) {
                System.err.println("[LibraryServer] Client disconnected: " + e.getMessage());
            } finally {
                if (writer != null) clients.remove(writer);
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();
    }

    public static void broadcast(String message) {
        String timestamped = "[" + LocalTime.now().withNano(0) + "] " + message;
        clients.removeIf(PrintWriter::checkError);
        for (PrintWriter writer : clients) {
            writer.println(timestamped);
        }
    }

    public static void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) {}
    }
}
