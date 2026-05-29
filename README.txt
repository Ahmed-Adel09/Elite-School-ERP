================================================================================
                           VERTEX ACADEMY ERP
================================================================================

PROJECT TITLE:
Vertex Academy ERP

GROUP MEMBERS:
1. Ahmed Adel
2. Omar Muhamed
3. El Hussein Mahumoed
4. Aya Sharaf
5. Eslam Fandokly

BUSINESS DOMAIN:
Elite School Management System. 
An integrated ERP (Enterprise Resource Planning) system for streamlined management 
across different user roles – Admin, HR, Teacher, Student, Parent, Librarian, 
Nurse, and Accountant.

================================================================================
                          FEATURES & TECHNOLOGIES
================================================================================

* MULTI-USER ROLES: Unique, personalized dashboards for all 8 distinct roles, 
  demonstrating deep understanding of Object-Oriented Programming (OOP) 
  Encapsulation, Inheritance, and Polymorphism.

* SECURE LOCKDOWN EXAMS: A Student portal feature utilizing aggressive JavaFX 
  Stage configuration (full-screen, always-on-top, window focus listeners) to 
  enforce strict academic integrity during assessments.

* UNIVERSAL ANNOUNCEMENTS: A modular, reusable UI component (OOP) designed to 
  fetch and display broadcast announcements to all users across the system.

* PARENT PORTAL INNOVATIONS:
  - Vertex AI Grade Analyzer: A weighted heuristic algorithm that processes 
    core grades to predict and recommend optimal college/career paths.
  - Private Sockets DMs: Advanced Socket Programming via Port 9998, enabling 
    secure, private 1-on-1 Parent-Teacher messaging.

* CLINIC & NURSE MODULES: Comprehensive medical profile tracking (CRUD), a 
  multithreaded medical inventory scanner running in the background, and 
  real-time Socket emergency alerts via Port 9997.

* FINANCIAL TRACKING: A dedicated Accountant portal for invoicing and payroll 
  management. Features an embedded, live FXGL bar chart that dynamically 
  visualizes "Collected Revenue" vs. "Pending Tuition."

* E-LIBRARY: Complete library management system supporting E-Book reading, 
  physical copy reservation (Socket broadcasts via Port 9997), a multithreaded 
  overdue scanner, and live FXGL library analytics.

* DATABASE: A centralized SQLite database file (elite_erp.db) managed entirely 
  via secure JDBC PreparedStatement queries to prevent SQL injection.

================================================================================
                    DEPLOYMENT & EXECUTION INSTRUCTIONS
================================================================================

1. PREREQUISITES
   - Java Development Kit (JDK) 17 or higher installed.
   - Apache Maven installed.
   - SQLite JDBC driver and AlmasB FXGL dependencies included in the pom.xml.

2. DATABASE SETUP
   The system automatically provisions its schema. Upon first launch, the 
   DatabaseManager class will execute and build all necessary SQLite tables 
   (elite_erp.db) and seed the initial users.

3. APPLICATION LAUNCH
   Navigate to the project root directory in your terminal and run the 
   application via Maven:
   
   mvn clean compile javafx:run

4. SOCKET DETAILS (CRITICAL)
   For the real-time communication features to function, the background server 
   threads are automatically launched on startup. 
   - Port 9998: Handles Chat Server & Private DMs.
   - Port 9997: Handles Library Reservations & Nurse Emergency Alerts.
   Ensure these ports are not blocked by your firewall.

5. DEFAULT LOGINS
   You can immediately test the application using the following pre-seeded 
   credentials:
   
   Role: Admin
   Email: admin111@gmail.com
   Password: oneaboveall

   Role: Accountant
   Email: acct111@gmail.com
   Password: finance123

   Role: School Nurse
   Email: nurse111@gmail.com
   Password: clinic123


   Role: HR
   Email: HR111@gmail.com
   Password: onebelowall

================================================================================
                          END OF DOCUMENT
================================================================================
