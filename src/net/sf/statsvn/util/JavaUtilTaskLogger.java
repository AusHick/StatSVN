package net.sf.statsvn.util;

import java.util.logging.Logger;

/**
 * Basic implementation to net.sf.statcvs logger.
 * 
 * @author Benoit Xhenseval
 * @version $Revision: 187 $
 */
public class JavaUtilTaskLogger implements TaskLogger {
	private static final Logger LOGGER = Logger.getLogger("net.sf.statcvs");

	/**
	 * log text to the logger.fine().
	 * 
	 * @param text
	 *            the text to log.
	 */
	public final void log(final String text) {
		LOGGER.fine(text);
	}

	/**
	 * log text to the logger.severe().
	 * 
	 * @param text
	 *            the text to log.
	 */
	public void error(final String arg) {
		LOGGER.severe(arg);
	}

	/**
	 * log text to the logger.info().
	 * 
	 * @param text
	 *            the text to log.
	 */
	public void info(final String arg) {
		LOGGER.info(arg);
	}
}
