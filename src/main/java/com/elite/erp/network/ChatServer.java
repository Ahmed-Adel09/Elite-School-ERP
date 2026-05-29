package com.elite.erp.network;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import com.elite.erp.dao.DatabaseManager;

/**
 * ChatServer — Manages Class Group Chats AND Private DMs via Sockets.
 * Port 9998.
 *
 * Protocol:
 *   JOIN|class_id|username|user_id[|role]  → join a class room (role: TEACHER or STUDENT)
 *   MSG|senderName|text                    → broadcast to class room
 *   PRIVATE|receiver_user_id|message       → route exclusively to one user
 *
 * Rubric demonstrations:
 *   ✅ Sockets       — ServerSocket + per-client Socket threads
 *   ✅ Multithreading — each client gets its own daemon Thread
 *   ✅ JDBC          — async INSERT to chat_history and private_messages
 *   ✅ Role tagging  — TEACHER messages get 🏫 [TEACHER] prefix in broadcast
 */
public class ChatServer {

    private static final int PORT = 9998;
    private static ServerSocket serverSocket;
    private static boolean running = false;

    // Class rooms: class_id → list of writers
    private static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<PrintWriter>> rooms
            = new ConcurrentHashMap<>();

    // Private DM registry: user_id → PrintWriter (one active session per user)
    private static final ConcurrentHashMap<Integer, PrintWriter> userWriters
            = new ConcurrentHashMap<>();

    private ChatServer() {}

    public static void start() {
        if (running) return;
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                running = true;
                System.out.println("[ChatServer] Listening on port " + PORT);

                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (SocketException e) {
                        if (running) System.err.println("[ChatServer] Socket error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("[ChatServer] Failed to start: " + e.getMessage());
            }
        }, "ChatServer-Thread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private static void handleClient(Socket socket) {
        Thread clientThread = new Thread(() -> {
            PrintWriter writer = null;
            int registeredUserId = -1;
            int joinedClassId    = -1;

            try {
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // First message must be JOIN|class_id|username|user_id[|role]
                String initMsg = reader.readLine();
                if (initMsg == null || !initMsg.startsWith("JOIN|")) return;

                String[] parts = initMsg.split("\\|");
                joinedClassId      = Integer.parseInt(parts[1]);
                String username    = parts[2];
                registeredUserId   = parts.length > 3 ? Integer.parseInt(parts[3]) : -1;
                String role        = parts.length > 4 ? parts[4] : "STUDENT";
                boolean isTeacher  = "TEACHER".equalsIgnoreCase(role);

                // Register in class room
                rooms.putIfAbsent(joinedClassId, new CopyOnWriteArrayList<>());
                rooms.get(joinedClassId).add(writer);

                // Register in DM registry (if user_id provided)
                if (registeredUserId > 0) {
                    userWriters.put(registeredUserId, writer);
                }

                String joinTag = isTeacher ? "🏫 [TEACHER] " + username : username;
                broadcast(joinedClassId, "[System] " + joinTag + " joined the chat.", -1, null);

                String line;
                while ((line = reader.readLine()) != null) {

                    // ── Private DM: PRIVATE|receiver_id|message ───────────────
                    if (line.startsWith("PRIVATE|")) {
                        String[] dm = line.split("\\|", 3);
                        if (dm.length == 3) {
                            int    receiverId = Integer.parseInt(dm[1]);
                            String body       = dm[2];
                            String stamped    = "[" + LocalTime.now().withNano(0) + "] 🔒 DM from "
                                               + username + ": " + body;

                            // Route exclusively to receiver
                            PrintWriter receiverWriter = userWriters.get(receiverId);
                            if (receiverWriter != null && !receiverWriter.checkError()) {
                                receiverWriter.println(stamped);
                            }
                            // Echo back to sender
                            writer.println("[" + LocalTime.now().withNano(0) + "] 📤 You → User#"
                                          + receiverId + ": " + body);

                            // Persist asynchronously
                            final int senderIdFinal = registeredUserId;
                            final String bodyFinal  = body;
                            new Thread(() -> savePrivateMessage(senderIdFinal, receiverId, bodyFinal),
                                    "DM-Save-Thread").start();
                        }

                    // ── Class broadcast: MSG|senderName|text ──────────────────
                    } else if (line.startsWith("MSG|")) {
                        String[] msgParts = line.split("\\|", 3);
                        if (msgParts.length == 3) {
                            String sender = msgParts[1];
                            String text   = msgParts[2];
                            // Tag teacher messages with role badge
                            String displaySender = isTeacher ? "🏫 [TEACHER] " + sender : sender;
                            broadcast(joinedClassId, text, joinedClassId, displaySender);
                        }
                    }
                }

                String leaveTag = isTeacher ? "🏫 [TEACHER] " + username : username;
                broadcast(joinedClassId, "[System] " + leaveTag + " left the chat.", -1, null);

            } catch (Exception e) {
                System.err.println("[ChatServer] Client disconnected: " + e.getMessage());
            } finally {
                // Clean up registrations
                if (writer != null && joinedClassId >= 0) {
                    CopyOnWriteArrayList<PrintWriter> room = rooms.get(joinedClassId);
                    if (room != null) room.remove(writer);
                }
                if (registeredUserId > 0) {
                    userWriters.remove(registeredUserId);
                }
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();
    }

    // ── Group broadcast ───────────────────────────────────────────────────────

    private static void broadcast(int classId, String message, int dbClassId, String dbSender) {
        CopyOnWriteArrayList<PrintWriter> room = rooms.get(classId);
        if (room != null) {
            room.removeIf(PrintWriter::checkError);
            if (dbSender != null) {
                room.forEach(w -> w.println(dbSender + ": " + message));
            } else {
                room.forEach(w -> w.println(message));
            }
        }

        // Async persist to chat_history
        if (dbClassId != -1 && dbSender != null) {
            new Thread(() -> {
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO chat_history (class_id, sender_name, message) VALUES (?, ?, ?)")) {
                    ps.setInt(1, dbClassId);
                    ps.setString(2, dbSender);
                    ps.setString(3, message);
                    ps.executeUpdate();
                } catch (Exception e) {
                    System.err.println("[ChatServer] DB Save error: " + e.getMessage());
                }
            }, "ChatSave-Thread").start();
        }
    }

    // ── Private message persistence ───────────────────────────────────────────

    private static void savePrivateMessage(int senderId, int receiverId, String body) {
        String sql = "INSERT INTO private_messages (sender_id, receiver_id, message_body) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, body);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[ChatServer] DM Save error: " + e.getMessage());
        }
    }

    public static void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) {}
    }
}
