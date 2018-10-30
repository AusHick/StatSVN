package net.sf.statsvn.util;

/**
 * An Interface for the Logging mechanism.
 * @author Benoit Xhenseval
 */
public interface TaskLogger {
	/**
	 * Generic interface for logging debug info.
	 * @param arg the string to log.
	 */
	void log(String arg);

	/**
	 * Generic interface for logging info.
	 * @param arg the string to log.
	 */
	void info(String arg);

	/**
	 * Generic interface for logging error.
	 * @param arg the string to log.
	 */
	void error(String arg);
}
