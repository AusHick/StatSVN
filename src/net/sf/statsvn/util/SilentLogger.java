package net.sf.statsvn.util;

/**
 * Basic implementation to nothingness.
 *
 * @author Benoit Xhenseval
 * @version $Revision$
 */
public class SilentLogger implements TaskLogger {
	/**
	 * log text to the System.out.
	 * @param text the text to log.
	 */
	public final void log(final String text) { // NOPMD
	}

	public void error(final String arg) { // NOPMD
	}

	public void info(final String arg) { // NOPMD
	}
}
