package com.elite.erp.business;

import com.elite.erp.dao.DatabaseManager;
import com.elite.erp.dao.StudentDAO;
import javafx.concurrent.Task;

import java.sql.Connection;
import java.sql.Statement;

/**
 * BackgroundSyncTask — demonstrates Multithreading requirement.
 * Extends JavaFX Task<Void> which runs on a background thread.
 * Simulates a database sync process (e.g. syncing local SQLite with
 * a remote server), updating a progress bar on the UI thread safely
 * via updateProgress() and updateMessage().
 */
public class BackgroundSyncTask extends Task<Void> {

    private static final int TOTAL_STEPS = 5;

    @Override
    protected Void call() throws Exception {
        updateTitle("Database Sync");
        updateMessage("Starting sync...");

        String[] steps = {
            "Verifying connection...",
            "Syncing student records...",
            "Syncing application data...",
            "Updating enrollment statistics...",
            "Sync complete!"
        };

        for (int i = 0; i < TOTAL_STEPS; i++) {
            if (isCancelled()) {
                updateMessage("Sync cancelled.");
                return null;
            }

            updateMessage(steps[i]);
            updateProgress(i + 1, TOTAL_STEPS);

            // Simulate step 2: re-validate DB connectivity
            if (i == 1) {
                try (Connection conn = DatabaseManager.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT 1");
                }
            }

            // Simulate step 3: count records
            if (i == 2) {
                StudentDAO dao = new StudentDAO();
                dao.count(); // triggers DB read
            }

            Thread.sleep(700); // simulate network/IO delay
        }

        updateMessage("✔  Sync complete — all records up to date.");
        return null;
    }

    @Override
    protected void cancelled() {
        updateMessage("Sync was cancelled.");
    }

    @Override
    protected void failed() {
        updateMessage("✖  Sync failed: " + getException().getMessage());
    }
}
