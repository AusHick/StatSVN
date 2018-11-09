package net.sf.statsvn.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.statcvs.util.LookaheadReader;
import net.sf.statsvn.output.SvnConfigurationOptions;

/**
 * Utility class that verifies if the correct version of subversion is used.
 * 
 * @author Jean-Philippe Daigle <jpdaigle@softwareengineering.ca>
 * 
 * @version $Id: SvnStartupUtils.java 394 2009-08-10 20:08:46Z jkealey $
 */
public class SvnStartupUtils implements ISvnVersionProcessor {
	private static final String SVN_VERSION_COMMAND = "svn --version";

	public static final String SVN_MINIMUM_VERSION = "1.3.10";

	public static final String SVN_MINIMUM_VERSION_DIFF_PER_REV = "1.4.0";

	private static final String SVN_VERSION_LINE_PATTERN = ".* [0-9]+\\.[0-9]+\\.[0-9]+.*";

	private static final String SVN_VERSION_PATTERN = "[0-9]+\\.[0-9]+\\.[0-9]+";


    protected ISvnProcessor processor;

    /**
     * Invokes various calls needed during StatSVN's startup, including the svn version command line.   
     */
    public SvnStartupUtils(ISvnProcessor processor) {
        this.processor = processor;
    }

    protected ISvnProcessor getProcessor() {
        return processor;
    }

	/* (non-Javadoc)
     * @see net.sf.statsvn.util.IVersionProcessor#checkSvnVersionSufficient()
     */
	public synchronized String checkSvnVersionSufficient() throws SvnVersionMismatchException {
		ProcessUtils pUtils = null;
		try {

			pUtils = ProcessUtils.call(SVN_VERSION_COMMAND);
			final InputStream istream = pUtils.getInputStream();
			final LookaheadReader reader = new LookaheadReader(new InputStreamReader(istream));

			while (reader.hasNextLine()) {
				final String line = reader.nextLine();
				if (line.matches(SVN_VERSION_LINE_PATTERN)) {
					// We have our version line
					final Pattern pRegex = Pattern.compile(SVN_VERSION_PATTERN);
					final Matcher m = pRegex.matcher(line);
					if (m.find()) {
						final String versionString = line.substring(m.start(), m.end());
						final String curVersion[] = versionString.split("\\.");
						final String minVersion[] = SVN_MINIMUM_VERSION.split("\\.");
						boolean versionSuccess = false;
						
						for (int i = 0; i < Math.min(minVersion.length, curVersion.length); i++) {
							final int curVersionNum = Integer.parseInt(curVersion[i]);
							final int minVersionNum = Integer.parseInt(minVersion[i]);
							
							if(curVersionNum == minVersionNum) {
								continue;
							} else if (curVersionNum > minVersionNum) {
								versionSuccess = true;
								break;
							} else if (curVersionNum < minVersionNum) {
								versionSuccess = false;
								break;
							}
						}
						
						if (versionSuccess) {
							return versionString;
						} else {
							throw new SvnVersionMismatchException(versionString, SVN_MINIMUM_VERSION);
						}
/*
						// we perform a simple string comparison against the version numbers
						if (versionString.compareTo(SVN_MINIMUM_VERSION) >= 0) {
							return versionString; // success
						} else {
							throw new SvnVersionMismatchException(versionString, SVN_MINIMUM_VERSION);
						}
*/
					}
				}
			}

			if (pUtils.hasErrorOccured()) {
				throw new IOException(pUtils.getErrorMessage());
			}
		} catch (final IOException e) {
			SvnConfigurationOptions.getTaskLogger().info(e.getMessage());
		} catch (final RuntimeException e) {
			SvnConfigurationOptions.getTaskLogger().info(e.getMessage());
		} finally {
			if (pUtils != null) {
				try {
					pUtils.close();
				} catch (final IOException e) {
					SvnConfigurationOptions.getTaskLogger().info(e.getMessage());
				}
			}
		}

		throw new SvnVersionMismatchException();
	}

	/* (non-Javadoc)
     * @see net.sf.statsvn.util.IVersionProcessor#checkDiffPerRevPossible(java.lang.String)
     */
	public synchronized boolean checkDiffPerRevPossible(final String version) {
		// we perform a simple string comparison against the version numbers
		return version.compareTo(SVN_MINIMUM_VERSION_DIFF_PER_REV) >= 0;
	}
}
