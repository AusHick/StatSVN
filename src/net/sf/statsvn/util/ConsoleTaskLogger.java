package net.sf.statsvn.util;

import java.io.PrintStream;

/**
 * Basic implementation to System.out.
 * 
 * @author Benoit Xhenseval
 * @version $Revision: 187 $
 */
public class ConsoleTaskLogger implements TaskLogger {
	private static final PrintStream STREAM = System.out;

	/**
	 * log text to the System.out.
	 * 
	 * @param text
	 *            the text to log.
	 */
	public final void log(final String text) {
		STREAM.println(text);
	}

	public void error(final String arg) {
		log(arg);
	}

	public void info(final String arg) {
		log(arg);
	}
}
