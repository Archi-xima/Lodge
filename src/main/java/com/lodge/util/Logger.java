package com.lodge.util;

/**
 * Simple logging framework for the lodge application.
 *
 * <p>A small, consistent facade over the SLF4J API (bound to Logback at run
 * time — see {@code logback.xml}). Team members use <b>this</b> class so no
 * screen or service ever talks to a logging back-end directly, and the
 * back-end can be swapped without touching business code.</p>
 *
 * <pre>{@code
 *   private static final Logger LOG = Logger.getLogger(RoomService.class);
 *
 *   LOG.info("Room 104 marked as occupied");
 *   LOG.debug("Loaded {} rooms from disk", rooms.size());
 *   LOG.error("Could not save the bill", exception);
 * }</pre>
 *
 * <p>Messages support SLF4J's {@code {}} placeholders. Output goes to the
 * console and to {@code ~/.lodge-billing/logs/lodge.log}.</p>
 */
public final class Logger {

    /** Fallback logger name when a caller has no usable name. */
    public static final String DEFAULT_LOGGER = "com.lodge";

    private final org.slf4j.Logger delegate;

    private Logger(String name) {
        this.delegate = org.slf4j.LoggerFactory.getLogger(name);
    }

    /** Logger named after the given class, e.g. {@code com.lodge.service.RoomService}. */
    public static Logger getLogger(Class<?> type) {
        return new Logger(type == null ? DEFAULT_LOGGER : type.getName());
    }

    /** Logger with an explicit name — useful in utilities and tests. */
    public static Logger getLogger(String name) {
        return new Logger(name == null || name.isBlank() ? DEFAULT_LOGGER : name);
    }

    /** Name this logger reports under. */
    public String getName() {
        return delegate.getName();
    }

    /** Trace-level diagnostic, cheapest to ignore. */
    public void trace(String message) {
        delegate.trace(message);
    }

    /** Detailed diagnostic aimed at developers. */
    public void debug(String message) {
        delegate.debug(message);
    }

    /** Detailed diagnostic with {@code {}} placeholders. */
    public void debug(String message, Object... arguments) {
        delegate.debug(message, arguments);
    }

    /** Normal "this happened" information. */
    public void info(String message) {
        delegate.info(message);
    }

    /** Normal information with {@code {}} placeholders. */
    public void info(String message, Object... arguments) {
        delegate.info(message, arguments);
    }

    /** Something recoverable that deserves attention. */
    public void warn(String message) {
        delegate.warn(message);
    }

    /** Something recoverable with {@code {}} placeholders. */
    public void warn(String message, Object... arguments) {
        delegate.warn(message, arguments);
    }

    /** A failure — always include the throwable so the stack trace is logged. */
    public void error(String message) {
        delegate.error(message);
    }

    /** A failure with its cause. */
    public void error(String message, Throwable cause) {
        delegate.error(message, cause);
    }

    /** A failure with {@code {}} placeholders and its cause. */
    public void error(String message, Throwable cause, Object... arguments) {
        delegate.error(message, arguments, cause);
    }

    /** Lets callers skip expensive string building when debug is off. */
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    /** Lets callers skip expensive string building when info is off. */
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }
}
