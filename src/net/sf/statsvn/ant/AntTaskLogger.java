package net.sf.statsvn.ant;

import net.sf.statcvs.output.ConfigurationOptions;
import net.sf.statsvn.util.TaskLogger;

import org.apache.tools.ant.Task;

/**
 * This class wraps up an Ant task which is going to be used to log some text
 * when the tool is used with Ant.
 * 
 * @author Benoit Xhenseval
 */
public final class AntTaskLogger implements TaskLogger {
	/** the Ant task. */
	private final Task task;

	private Boolean shouldAcceptLog = null;

	private Boolean shouldAcceptInfo = null;

	/**
	 * Constructor that will hide the specific logging mechanism.
	 * 
	 * @param antTask
	 *            an Ant task
	 */
	AntTaskLogger(final Task antTask) {
		this.task = antTask;
	}

	/**
	 * Uses the Ant mechanism to log the text.
	 * 
	 * @param text
	 *            to be logged.
	 */
	public void log(final String text) {
		if (shouldAcceptLog == null) {
			shouldAcceptLog = Boolean.valueOf(ConfigurationOptions.getLoggingProperties().indexOf("debug") >= 0);
		}
		if (shouldAcceptLog.booleanValue()) {
			task.log(text);
		}
	}

	/**
	 * Uses the Ant mechanism to log the text.
	 * 
	 * @param text
	 *            to be logged.
	 */
	public void error(final String arg) {
		log(arg);
	}

	/**
	 * Uses the Ant mechanism to log the text.
	 * 
	 * @param text
	 *            to be logged.
	 */
	public void info(final String arg) {
		if (shouldAcceptInfo == null) {
			shouldAcceptInfo = Boolean.valueOf(ConfigurationOptions.getLoggingProperties().indexOf("verbose") >= 0);
		}
		if (shouldAcceptInfo.booleanValue()) {
			log(arg);
		}
	}
}
