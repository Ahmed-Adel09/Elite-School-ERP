package com.elite.erp.business;

import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * BaseService — abstract parent for all services.
 * Demonstrates OOP Inheritance: subclasses inherit logging, validation,
 * and common utility methods without code duplication.
 */
public abstract class BaseService {

    protected final Logger logger;

    protected BaseService() {
        this.logger = Logger.getLogger(getClass().getName());
    }

    // ── Shared validation utilities (inherited by all services) ─────────────

    protected boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    protected boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }

    protected boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^[+]?[0-9]{7,15}$");
    }

    protected void logInfo(String message) {
        logger.info("[" + LocalDateTime.now() + "] " + message);
    }

    protected void logError(String message, Exception e) {
        logger.severe("[" + LocalDateTime.now() + "] " + message
                + (e != null ? " | " + e.getMessage() : ""));
    }
}
