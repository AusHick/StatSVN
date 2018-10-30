package net.sf.statsvn.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import net.sf.statcvs.Messages;

/**
 * This class provides a report to standard output of relevant JRE properties
 * and svn executable information. This output is intended to be included with
 * bug reports to help the StatSVN team diagnose common issues with wrong SVN
 * version, wrong JRE version, and locale settings.
 * 
 * @author jpdaigle jpdaigle@softwareengineering.ca
 * 
 */
public final class EnvReport {
	private static String[] envPropKeys = { "file.encoding", "java.home", "java.runtime.version", "os.arch", "os.name", "os.version", "user.country",
	        "user.language" };

	static final String SVN_VERSION_COMMAND = "svn --version";

	static final String SVN_VERSION_LINE_PATTERN = ".* [0-9]+\\.[0-9]+\\.[0-9]+.*";

	static final String KEY_SVN_ABLE_TO_RUN = "svn.able.to.run";

	static final String KEY_SVN_VERSION = "svn.reportedversion";

	static final String KEY_STATSVN_VERSION = "statsvn.reportedversion";

	private EnvReport() {
		// no public ctor
	}

	public static void main(final String[] args) {
		System.out.println(getEnvReport());
	}

	public static String getEnvReport() {
		final StringBuffer buf = new StringBuffer();
		buf.append("\nWhen reporting a StatSVN bug or requesting assistance,\n");
		buf.append("please include the entirety of the output below.\n");
		buf.append("No personally-identifiable information is included.\n\n");

		buf.append("=== Java Runtime Properties ===\n");
		buf.append(fmtPropertiesForScreen(System.getProperties(), envPropKeys));

		buf.append("\n");
		buf.append("=== Subversion Properties ===\n");
		buf.append(fmtPropertiesForScreen(getSvnVersionInfo(), null));

		buf.append("\n");
		buf.append("=== StatSVN Properties ===\n");
		buf.append(fmtPropertiesForScreen(getStatSVNInfo(), null));

		return buf.toString();
	}

	/**
	 * Format a set of key/value Properties for the screen, by right-aligning
	 * the key column. Each key/value pair is printed and followed by a newline.
	 * 
	 * @author jpdaigle
	 * @param props
	 *            Property set to format for printout.
	 * @param keySet
	 *            The keys of interest to use. If null, use all keys defined in
	 *            props.
	 * @return Formatted text block with property keys and values, with a
	 *         newline after every set.
	 */
	public static String fmtPropertiesForScreen(final Properties props, String[] keySet) {
		int maxWidth = 0;
		final StringBuffer buf = new StringBuffer();
		if (keySet == null) {
			keySet = (String[]) props.keySet().toArray(new String[props.keySet().size()]);
		}
		final Vector vKeys = new Vector(Arrays.asList(keySet));
		Collections.sort(vKeys);

		// First pass: find length of longest key
		for (final Iterator ite = vKeys.iterator(); ite.hasNext();) {
			final String key = ((String) ite.next()).trim();
			maxWidth = (key.length() > maxWidth) ? key.length() : maxWidth;
		}

		// Second pass: output formatted keys / values
		for (final Iterator ite = vKeys.iterator(); ite.hasNext();) {
			final String key = ((String) ite.next()).trim();
			for (int i = maxWidth - key.length(); i > 0; i--) {
				buf.append(" ");
			}
			buf.append(key).append(":[").append(props.getProperty(key).trim()).append("]\n");
		}

		return buf.toString();
	}

	/**
	 * Get svn executable version info. We cannot use the excellent
	 * "ProcessUtils" because we are not running in the context of a StatSVN
	 * invocation. We use a plain old exec().
	 * 
	 * @return Property set
	 */
	public static Properties getSvnVersionInfo() {

		String versionLine = "";
		String line;
		final Properties svnProps = new Properties();
		BufferedReader input = null;

		try {
			svnProps.setProperty(KEY_SVN_ABLE_TO_RUN, "YES");
			final Process proc = Runtime.getRuntime().exec(SVN_VERSION_COMMAND);

			input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			while ((line = input.readLine()) != null) {
				if (line.matches(SVN_VERSION_LINE_PATTERN)) {
					// We have our version line
					versionLine = line.trim();
					break;
				}
			}
		} catch (final Exception e) {
			svnProps.setProperty(KEY_SVN_ABLE_TO_RUN, "NO: " + e.getMessage().trim());
		} finally {
			svnProps.setProperty(KEY_SVN_VERSION, versionLine);
			if (input != null) {
				try {
					input.close();
				} catch (final IOException ex) {
					// swallow it
					ex.printStackTrace();
				}
			}
		}
		return svnProps;
	}

	/**
	 * Get information about the current version of StatSVN.
	 * @return Property set
	 */
	public static Properties getStatSVNInfo() {
		final Properties statsvnProps = new Properties();
		statsvnProps.setProperty(KEY_STATSVN_VERSION, Messages.getString("PROJECT_VERSION"));
		return statsvnProps;
	}

}
