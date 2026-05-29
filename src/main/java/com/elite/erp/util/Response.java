package com.elite.erp.util;

/**
 * Generic response wrapper — demonstrates Java Generics.
 * Used throughout the DAO and Service layers to wrap results
 * with success/failure metadata.
 *
 * @param <T> The type of data carried in this response
 */
public class Response<T> {

    private final boolean success;
    private final String  message;
    private final T       data;
    private final String  errorCode;

    // ── Private constructor (use factory methods) ────────────────────────────
    private Response(boolean success, String message, T data, String errorCode) {
        this.success   = success;
        this.message   = message;
        this.data      = data;
        this.errorCode = errorCode;
    }

    // ── Factory methods ──────────────────────────────────────────────────────
    public static <T> Response<T> success(T data, String message) {
        return new Response<>(true, message, data, null);
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(true, "Operation successful", data, null);
    }

    public static <T> Response<T> failure(String message) {
        return new Response<>(false, message, null, "ERR_GENERIC");
    }

    public static <T> Response<T> failure(String message, String errorCode) {
        return new Response<>(false, message, null, errorCode);
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public boolean isSuccess()   { return success; }
    public String  getMessage()  { return message; }
    public T       getData()     { return data; }
    public String  getErrorCode(){ return errorCode; }

    @Override
    public String toString() {
        return "Response{success=" + success + ", message='" + message + "'}";
    }
}
