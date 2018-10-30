package net.sf.statsvn.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.sf.statcvs.util.LookaheadReader;
import net.sf.statsvn.output.SvnConfigurationOptions;

/**
 * This class provides a way of launching new processes. It is not the best way
 * and it surely does not work well in multi-threaded environments. It is
 * sufficient for StatSVN's single thread.
 * 
 * We should be launching two threads with readers for both, but we are lazy.
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps_p.html
 * 
 * @author jkealey <jkealey@shade.ca>
 * 
 */
public final class ProcessUtils {
	private BufferedInputStream inputStream;

	private BufferedInputStream errorStream;

	/**
	 * A utility class (only static methods) should be final and have
	 * a private constructor.
	 */
	public ProcessUtils() {
	}

	public static synchronized ProcessUtils call(final String sCommand) throws IOException {
		final ProcessUtils util = new ProcessUtils();
		final Process lastProcess = Runtime.getRuntime().exec(sCommand, null, getWorkingFolder());
		util.errorStream = new BufferedInputStream(lastProcess.getErrorStream());
		util.inputStream = new BufferedInputStream(lastProcess.getInputStream());

		return util;
	}

	public void close() throws IOException {
		if (errorStream != null) {
			errorStream.close();
			errorStream = null;
		}
		if (inputStream != null) {
			inputStream.close();
			inputStream = null;
		}
	}

	private static File getWorkingFolder() {
		return SvnConfigurationOptions.getCheckedOutDirectoryAsFile();
	}

	protected boolean hasErrorOccured() throws IOException {
		return errorStream != null && errorStream.available() > 0;
	}

	protected String getErrorMessage() {
		if (errorStream == null) {
			return null;
		} else {
			final LookaheadReader diffReader = new LookaheadReader(new InputStreamReader(errorStream));
			final StringBuffer builder = new StringBuffer();
			try {
				while (diffReader.hasNextLine()) {
					builder.append(diffReader.nextLine());
				}
			} catch (final IOException e) {
				SvnConfigurationOptions.getTaskLogger().error(e.toString());
			}

			return builder.toString();
		}
	}

	/**
	 * @return the errorStream
	 */
	public BufferedInputStream getErrorStream() {
		return errorStream;
	}

	/**
	 * @return the inputStream
	 */
	public BufferedInputStream getInputStream() {
		return inputStream;
	}

	/**
	 * @param errorStream the errorStream to set
	 */
	public void setErrorStream(final InputStream errorStream) {
		this.errorStream = new BufferedInputStream(errorStream);
	}

	/**
	 * @param inputStream the inputStream to set
	 */
	public void setInputStream(final InputStream inputStream) {
		this.inputStream = new BufferedInputStream(inputStream);
	}
}
