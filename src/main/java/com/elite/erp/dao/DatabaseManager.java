package com.elite.erp.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC Database Manager — Singleton pattern.
 * Uses SQLite (local file-based SQL) — no server installation required.
 * Database file: ~/elite_erp.db
 *
 * Admin credentials:  username = admin   password = admin123
 */
public class DatabaseManager {

    private static final String DB_PATH  =
            System.getProperty("user.home") + "/elite_erp.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;


    private DatabaseManager() {}

    // ── Connection factory ───────────────────────────────────────────────────
    // Returns a BRAND NEW connection every call.
    //
    // WHY: The old singleton broke with try-with-resources because the first
    // caller's `try(Connection c = getConnection())` block would close() the
    // one shared connection object on exit, leaving every subsequent call with
    // a closed connection.  SQLite file-based connections are cheap to open, so
    // just create a fresh one each time — each caller owns and closes its own.
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(JDBC_URL);
        conn.setAutoCommit(true);
        return conn;
    }

    // ── Schema initialisation ────────────────────────────────────────────────
    public static void initializeSchema() {

        // Enable WAL mode for better concurrency
        String walMode = "PRAGMA journal_mode=WAL;";

        String createApplications = """
            CREATE TABLE IF NOT EXISTS applications (
                id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                status              TEXT    DEFAULT 'PENDING',
                submission_date     TEXT,
                payment_amount      REAL    DEFAULT 200.0,
                payment_completed   INTEGER DEFAULT 0,
                language            TEXT    DEFAULT 'EN',
                needs_password_setup INTEGER DEFAULT 0
            );
            """;

        String createStudents = """
            CREATE TABLE IF NOT EXISTS students (
                id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                application_id     INTEGER,
                first_name         TEXT,
                last_name          TEXT,
                date_of_birth      TEXT,
                gender             TEXT,
                nationality        TEXT,
                applying_for_grade TEXT,
                previous_school    TEXT,
                previous_gpa       TEXT,
                photo_path         TEXT,
                allergies          TEXT,
                medical_notes      TEXT,
                vaccination_record TEXT,
                age_flagged        INTEGER DEFAULT 0,
                student_email      TEXT,
                class_id           INTEGER,
                FOREIGN KEY (application_id) REFERENCES applications(id),
                FOREIGN KEY (class_id) REFERENCES classes(id)
            );
            """;

        String createParents = """
            CREATE TABLE IF NOT EXISTS parents (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                application_id    INTEGER,
                father_name       TEXT,
                mother_name       TEXT,
                email             TEXT,
                phone             TEXT,
                address           TEXT,
                occupation        TEXT,
                corporate_sponsor INTEGER DEFAULT 0,
                company_name      TEXT,
                company_email     TEXT,
                sponsor_amount    REAL    DEFAULT 0.0,
                FOREIGN KEY (application_id) REFERENCES applications(id)
            );
            """;

        String createEnrollmentStats = """
            CREATE TABLE IF NOT EXISTS enrollment_stats (
                id    INTEGER PRIMARY KEY AUTOINCREMENT,
                month TEXT,
                year  INTEGER,
                count INTEGER DEFAULT 0
            );
            """;

        String createUsers = """
            CREATE TABLE IF NOT EXISTS users (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                username  TEXT UNIQUE NOT NULL,
                password  TEXT,
                role      TEXT DEFAULT 'ADMIN',
                full_name TEXT,
                email     TEXT
            );
            """;

        String createTransactions = """
            CREATE TABLE IF NOT EXISTS transactions (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                application_id INTEGER,
                ipa_reference  TEXT,
                amount         REAL    DEFAULT 200.0,
                status         TEXT    DEFAULT 'PENDING',
                verified_at    TEXT,
                FOREIGN KEY (application_id) REFERENCES applications(id)
            );
            """;

        String createGrades = """
            CREATE TABLE IF NOT EXISTS grades (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER,
                subject    TEXT,
                score      REAL,
                date       TEXT
            );
            """;

        String createAttendance = """
            CREATE TABLE IF NOT EXISTS attendance (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER,
                date       TEXT,
                status     TEXT
            );
            """;

        String createCredits = """
            CREATE TABLE IF NOT EXISTS credits (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                parent_id INTEGER UNIQUE,
                balance   REAL DEFAULT 0.0
            );
            """;

        String createEvents = """
            CREATE TABLE IF NOT EXISTS school_events (
                id   INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                cost REAL,
                date TEXT
            );
            """;

        String createAnnouncements = """
            CREATE TABLE IF NOT EXISTS announcements (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                title           TEXT,
                content         TEXT,
                date_posted     TEXT,
                media_path      TEXT,
                target_audience TEXT DEFAULT 'ALL'
            );
            """;

        String createAssets = """
            CREATE TABLE IF NOT EXISTS assets (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER,
                item_name  TEXT
            );
            """;

        String createConsent = """
            CREATE TABLE IF NOT EXISTS consent (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER,
                form_name  TEXT,
                signed     INTEGER DEFAULT 0
            );
            """;

        String createTeacherApplications = """
            CREATE TABLE IF NOT EXISTS teacher_applications (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name        TEXT,
                email            TEXT,
                phone            TEXT,
                subject          TEXT,
                experience_years INTEGER,
                cv_path          TEXT,
                status           TEXT DEFAULT 'PENDING'
            );
            """;

        // New unified staff applications table (covers ALL roles via discriminator)
        String createStaffApplications = """
            CREATE TABLE IF NOT EXISTS staff_applications (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name  TEXT    NOT NULL,
                email      TEXT    NOT NULL,
                phone      TEXT,
                cv_path    TEXT,
                role_type  TEXT    NOT NULL,
                extra_data TEXT,
                status     TEXT    DEFAULT 'PENDING',
                created_at TEXT    DEFAULT (datetime('now'))
            );
            """;

        String createClasses = """
            CREATE TABLE IF NOT EXISTS classes (
                id   INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL
            );
            """;

        String createTeacherClasses = """
            CREATE TABLE IF NOT EXISTS teacher_classes (
                user_id  INTEGER,
                class_id INTEGER,
                PRIMARY KEY (user_id, class_id),
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (class_id) REFERENCES classes(id)
            );
            """;

        String createAssignments = """
            CREATE TABLE IF NOT EXISTS assignments (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id   INTEGER,
                teacher_id INTEGER,
                title      TEXT,
                file_path  TEXT,
                created_at TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (class_id) REFERENCES classes(id),
                FOREIGN KEY (teacher_id) REFERENCES users(id)
            );
            """;

        String createExams = """
            CREATE TABLE IF NOT EXISTS exams (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id         INTEGER,
                teacher_id       INTEGER,
                title            TEXT,
                duration_minutes INTEGER,
                expiry_timestamp TEXT,
                created_at       TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (class_id) REFERENCES classes(id),
                FOREIGN KEY (teacher_id) REFERENCES users(id)
            );
            """;

        String createExamQuestions = """
            CREATE TABLE IF NOT EXISTS exam_questions (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                exam_id        INTEGER,
                question_text  TEXT,
                option_a       TEXT,
                option_b       TEXT,
                option_c       TEXT,
                option_d       TEXT,
                correct_option TEXT,
                FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
            );
            """;

        String createChatHistory = """
            CREATE TABLE IF NOT EXISTS chat_history (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id    INTEGER,
                sender_name TEXT,
                message     TEXT,
                timestamp   TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (class_id) REFERENCES classes(id)
            );
            """;

        String createLibraryBooks = """
            CREATE TABLE IF NOT EXISTS library_books (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                title           TEXT,
                author          TEXT,
                category        TEXT,
                type            TEXT,
                file_path       TEXT,
                total_stock     INTEGER,
                available_stock INTEGER
            );
            """;

        String createBookLoans = """
            CREATE TABLE IF NOT EXISTS book_loans (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id      INTEGER,
                student_id   INTEGER,
                request_date TEXT DEFAULT (datetime('now')),
                due_date     TEXT,
                status       TEXT DEFAULT 'PENDING',
                FOREIGN KEY (book_id) REFERENCES library_books(id),
                FOREIGN KEY (student_id) REFERENCES users(id)
            );
            """;

        // ── Medical / Clinic tables ───────────────────────────────────────────
        String createMedicalProfiles = """
            CREATE TABLE IF NOT EXISTS medical_profiles (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id        INTEGER UNIQUE,
                blood_type        TEXT,
                allergies         TEXT,
                emergency_contact TEXT,
                FOREIGN KEY (student_id) REFERENCES students(id)
            );
            """;

        String createClinicVisits = """
            CREATE TABLE IF NOT EXISTS clinic_visits (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id       INTEGER,
                date_time        TEXT DEFAULT (datetime('now')),
                symptoms         TEXT,
                treatment_given  TEXT,
                nurse_id         INTEGER,
                FOREIGN KEY (student_id) REFERENCES students(id),
                FOREIGN KEY (nurse_id)   REFERENCES users(id)
            );
            """;

        String createMedicalInventory = """
            CREATE TABLE IF NOT EXISTS medical_inventory (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                item_name        TEXT UNIQUE,
                current_stock    INTEGER DEFAULT 0,
                minimum_required INTEGER DEFAULT 5
            );
            """;

        // ── Private Messaging ─────────────────────────────────────────────────
        String createPrivateMessages = """
            CREATE TABLE IF NOT EXISTS private_messages (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                sender_id    INTEGER,
                receiver_id  INTEGER,
                message_body TEXT,
                timestamp    TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (sender_id)   REFERENCES users(id),
                FOREIGN KEY (receiver_id) REFERENCES users(id)
            );
            """;

        // ── Financial / Accountant tables ─────────────────────────────────────
        String createInvoices = """
            CREATE TABLE IF NOT EXISTS invoices (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id   INTEGER,
                amount       REAL    DEFAULT 0.0,
                status       TEXT    DEFAULT 'PENDING',
                issue_date   TEXT    DEFAULT (date('now')),
                description  TEXT,
                FOREIGN KEY (student_id) REFERENCES students(id)
            );
            """;

        // ── Class Timetable / Schedule ──────────────────────────────────────────
        String createClassSchedule = """
            CREATE TABLE IF NOT EXISTS class_schedule (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id         INTEGER NOT NULL,
                subject          TEXT    NOT NULL,
                subject_type     TEXT    DEFAULT 'CORE',
                day_1            TEXT,
                day_2            TEXT,
                day_3            TEXT,
                duration_minutes INTEGER DEFAULT 60,
                start_time       TEXT    DEFAULT '08:30',
                FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
            );
            """;

        // ── Seed data ────────────────────────────────────────────────────────
        String seedStats = """
            INSERT OR IGNORE INTO enrollment_stats (id, month, year, count) VALUES
            (1,'Jan',2025,12),(2,'Feb',2025,18),(3,'Mar',2025,25),
            (4,'Apr',2025,22),(5,'May',2025,30),(6,'Jun',2025,15),
            (7,'Jul',2025,8),(8,'Aug',2025,40),(9,'Sep',2025,55),
            (10,'Oct',2025,48),(11,'Nov',2025,35),(12,'Dec',2025,20);
            """;

        /*
         * Admin account seed — System Administrator role.
         * Credentials:  admin111@gmail.com / oneaboveall
         */
        String seedAdmin = """
            INSERT OR REPLACE INTO users (id, username, password, role, full_name, email)
            VALUES (1, 'admin111@gmail.com', 'oneaboveall', 'ADMIN',
                    'System Administrator', 'admin111@gmail.com');
            """;

        /*
         * HR account seed — Human Resources role.
         * Credentials:  HR111@gmail.com / onebelowall
         */
        String seedHR = """
            INSERT OR IGNORE INTO users (username, password, role, full_name, email)
            VALUES ('HR111@gmail.com', 'onebelowall', 'HR',
                    'HR Manager', 'HR111@gmail.com');
            """;

        String seedLibrarian = """
            INSERT OR IGNORE INTO users (username, password, role, full_name, email)
            VALUES ('lib111@gmail.com', 'bookworm', 'LIBRARIAN',
                    'Head Librarian', 'lib111@gmail.com');
            """;

        String seedNurse = """
            INSERT OR IGNORE INTO users (username, password, role, full_name, email)
            VALUES ('nurse111@gmail.com', 'clinic123', 'NURSE',
                    'School Nurse', 'nurse111@gmail.com');
            """;

        String seedAccountant = """
            INSERT OR IGNORE INTO users (username, password, role, full_name, email)
            VALUES ('acct111@gmail.com', 'finance123', 'ACCOUNTANT',
                    'Finance Officer', 'acct111@gmail.com');
            """;

        String seedMedicalInventory = """
            INSERT OR IGNORE INTO medical_inventory (item_name, current_stock, minimum_required) VALUES
            ('EpiPens',         3,  2),
            ('Paracetamol',    50,  10),
            ('Bandages',       80,  20),
            ('Antiseptic Spray', 5, 3),
            ('Asthma Inhaler',  1,  2);
            """;

        String seedEvents = """
            INSERT OR IGNORE INTO school_events (id, name, cost, date) VALUES
            (1, 'Spring Science Museum Trip', 50.0, '2025-04-15'),
            (2, 'Robotics Club Semester Fee', 150.0, '2025-02-01'),
            (3, 'Annual School Play Ticket', 25.0, '2025-05-20');
            """;

        String seedAnnouncements = """
            INSERT OR IGNORE INTO announcements (id, title, content, date_posted, target_audience) VALUES
            (1, 'Welcome to the New Semester', 'We are excited to welcome all students back to campus.', '2025-01-10', 'ALL'),
            (2, 'Upcoming Parent-Teacher Conferences', 'Conferences will be held next week.', '2025-02-05', 'PARENT');
            """;

        // Grades are NOT seeded — they are inserted live when students submit exams.
        // See LockdownExamController.java → submitExam()

        String seedClasses = """
            INSERT OR IGNORE INTO classes (id, name) VALUES
            (1, '10A'), (2, '10B'), (3, '10C'), (4, '10D'), (5, '10E'),
            (6, '11A'), (7, '11B'), (8, '11C'), (9, '11D'), (10, '11E'),
            (11, '12A'), (12, '12B'), (13, '12C'), (14, '12D'), (15, '12E');
            """;

        // ALTER TABLE — silently ignored if column already exists
        String[] alterStudents = {
            "ALTER TABLE students ADD COLUMN age_flagged    INTEGER DEFAULT 0;",
            "ALTER TABLE students ADD COLUMN student_email  TEXT;"
        };
        String[] alterApplications = {
            "ALTER TABLE applications ADD COLUMN needs_password_setup INTEGER DEFAULT 0;"
        };

        try (Connection conn = getConnection();
             Statement  stmt = conn.createStatement()) {

            stmt.execute(walMode);
            stmt.execute(createApplications);
            stmt.execute(createStudents);
            stmt.execute(createParents);
            stmt.execute(createEnrollmentStats);
            stmt.execute(createUsers);
            stmt.execute(createTransactions);
            
            // New tables
            stmt.execute(createGrades);
            stmt.execute(createAttendance);
            stmt.execute(createCredits);
            stmt.execute(createEvents);
            stmt.execute(createAnnouncements);
            stmt.execute(createAssets);
            stmt.execute(createConsent);
            stmt.execute(createTeacherApplications);
            stmt.execute(createStaffApplications);
            stmt.execute(createClasses);
            stmt.execute(createTeacherClasses);
            stmt.execute(createAssignments);
            stmt.execute(createExams);
            stmt.execute(createExamQuestions);
            stmt.execute(createChatHistory);
            stmt.execute(createLibraryBooks);
            stmt.execute(createBookLoans);

            // Medical / Clinic tables
            stmt.execute(createMedicalProfiles);
            stmt.execute(createClinicVisits);
            stmt.execute(createMedicalInventory);

            // Messaging & Financial tables
            stmt.execute(createPrivateMessages);
            stmt.execute(createInvoices);
            stmt.execute(createClassSchedule);

            // ── Migration: add start_time if upgrading an existing database ──────
            try { stmt.execute("ALTER TABLE class_schedule ADD COLUMN start_time TEXT DEFAULT '08:30'"); }
            catch (SQLException ignored) { /* column already exists — safe to ignore */ }

            // Seeders
            stmt.execute(seedStats);
            stmt.execute(seedAdmin);
            stmt.execute(seedHR);
            stmt.execute(seedLibrarian);
            stmt.execute(seedNurse);
            stmt.execute(seedAccountant);
            stmt.execute(seedEvents);
            stmt.execute(seedAnnouncements);
            // Remove old hardcoded fake grades (Math/Science/History/English for student_id=1)
            try { stmt.execute("DELETE FROM grades WHERE id IN (1,2,3,4) AND student_id = 1 AND subject IN ('Math','Science','History','English')"); } catch (Exception ignored) {}
            stmt.execute(seedClasses);
            stmt.execute(seedMedicalInventory);

            // Migrate existing DB — add missing columns without dropping data
            for (String alter : alterStudents) {
                try { stmt.execute(alter); }
                catch (SQLException ignored) { /* column already exists */ }
            }
            for (String alter : alterApplications) {
                try { stmt.execute(alter); }
                catch (SQLException ignored) { /* column already exists */ }
            }

            System.out.println("[DB] Schema ready. DB path: " + DB_PATH);
            System.out.println("[DB] Admin login → email: admin111@gmail.com  password: oneaboveall");
            System.out.println("[DB] HR login    → email: HR111@gmail.com     password: onebelowall");

        } catch (SQLException e) {
            System.err.println("[DB] Schema init failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void factoryResetDatabase() {
        String[] tables = {
            "applications", "students", "parents", "enrollment_stats", "users",
            "transactions", "grades", "attendance", "credits", "school_events",
            "announcements", "assets", "consent", "teacher_applications",
            "staff_applications", "classes", "teacher_classes", "assignments",
            "exams", "exam_questions", "chat_history", "library_books",
            "book_loans", "medical_profiles", "clinic_visits", "medical_inventory",
            "private_messages", "invoices", "class_schedule"
        };

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("[DB] Commencing Factory Reset... Dropping all tables.");

            // Disable foreign keys temporarily during drop
            stmt.execute("PRAGMA foreign_keys = OFF;");

            for (String table : tables) {
                stmt.execute("DROP TABLE IF EXISTS " + table + ";");
            }

            stmt.execute("PRAGMA foreign_keys = ON;");
            System.out.println("[DB] All tables dropped successfully.");

        } catch (SQLException e) {
            System.err.println("[DB] Failed to drop tables: " + e.getMessage());
        }

        // Rebuild empty schema (and seeds)
        initializeSchema();

        // Ensure Master Admin exists
        String insertMasterAdmin = """
            INSERT INTO users (username, password, role, full_name, email)
            VALUES ('admin@vertex.edu', 'admin', 'ADMIN', 'Master Admin', 'admin@vertex.edu')
            """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(insertMasterAdmin);
            System.out.println("[DB] Master Admin account created (admin@vertex.edu / admin).");
        } catch (SQLException e) {
            System.err.println("[DB] Failed to create Master Admin: " + e.getMessage());
        }
    }

    /** No-op: connections are now per-call (try-with-resources closes each one). */
    public static void closeConnection() {
        // Nothing to do — each getConnection() call returns its own connection
        // which try-with-resources closes automatically when the block exits.
    }
}
