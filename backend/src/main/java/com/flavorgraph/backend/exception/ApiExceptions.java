package com.flavorgraph.backend.exception;

public final class ApiExceptions {
    private ApiExceptions() {}
    public static class NotFound extends RuntimeException { public NotFound(String message) { super(message); } }
    public static class DatabaseUnavailable extends RuntimeException {
        public DatabaseUnavailable(Throwable cause) { super("FlavorGraph is temporarily unable to reach its recipe database.", cause); }
    }
}
