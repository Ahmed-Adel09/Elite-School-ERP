package com.elite.erp.network;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * NotificationClient — demonstrates Socket Programming (client side).
 *
 * Connects to NotificationServer on port 9999 in a background thread.
 * Received messages are dispatched to a JavaFX consumer callback
 * safely via Platform.runLater(), so the UI can display toast notifications.
 */
public class NotificationClient {

    private static final String HOST = "localhost";
    private static final int    PORT = 9999;

    private Socket       socket;
    private PrintWriter  writer;
    private boolean      connected = false;
    private Thread       listenerThread;

    /**
     * Connect and start listening for notifications.
     *
     * @param onMessage callback invoked on JavaFX thread when a message arrives
     */
    public void connect(Consumer<String> onMessage) {
        listenerThread = new Thread(() -> {
            // Retry up to 5 times if server hasn't started yet
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    socket = new Socket(HOST, PORT);
                    writer = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream()), true);
                    connected = true;
                    System.out.println("[Client] Connected to NotificationServer");

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String msg = line;
                        Platform.runLater(() -> onMessage.accept(msg));
                    }
                    break; // clean disconnect

                } catch (ConnectException e) {
                    System.out.println("[Client] Attempt " + attempt + " — server not ready, retrying...");
                    try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) { break; }
                } catch (IOException e) {
                    System.err.println("[Client] IO error: " + e.getMessage());
                    break;
                }
            }
        }, "NotificationClient-Thread");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /** Send a message to the server (e.g., ping or client command) */
    public void send(String message) {
        if (connected && writer != null) {
            writer.println(message);
        }
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); }
        catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
}
