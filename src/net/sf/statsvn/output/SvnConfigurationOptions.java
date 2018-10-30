/*
 StatCvs - CVS statistics generation
 Copyright (C) 2002  Lukasz Pekacki <lukasz@pekacki.de>
 http://statcvs.sf.net/

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 $RCSfile: ConfigurationOptions.java,v $
 $Date: 2005/03/20 19:12:25 $
 */
package net.sf.statsvn.output;

import java.io.File;

import net.sf.statcvs.output.ConfigurationException;
import net.sf.statcvs.output.ConfigurationOptions;
import net.sf.statcvs.util.FileUtils;
import net.sf.statsvn.util.ISvnProcessor;
import net.sf.statsvn.util.JavaUtilTaskLogger;
import net.sf.statsvn.util.SvnCommandLineProcessor;
import net.sf.statsvn.util.TaskLogger;
import net.sf.statsvn.util.svnkit.SvnKitProcessor;

/**
 * Class for storing all command line parameters. The parameters are set by the
 * {@link net.sf.statsvn.Main#main} method. Interested classes can read all
 * parameter values from here.
 * 
 * @todo Should be moved to more appropriate package and made non-public
 * 
 * @author jentzsch
 * @version $Id: ConfigurationOptions.java,v 1.17 2005/03/20 19:12:25 squig Exp $
 */
public final class SvnConfigurationOptions {
	private static final int DEFAULT_THRESHOLD_MS_FOR_CONCURRENCY = 2000;

	private static final int DEFAULT_NUMBER_THREADS = 25;

	private static String cacheDir = "";

	private static final String DEFAULT_CACHE_DIR = System.getProperty("user.home") + FileUtils.getDirSeparator() + ".statsvn" + FileUtils.getDirSeparator();

	private static String svnUsername = null;

	private static String svnPassword = null;

	private static TaskLogger taskLogger = new JavaUtilTaskLogger();

	private static int numberSvnDiffThreads = DEFAULT_NUMBER_THREADS;

	private static long thresholdInMsToUseConcurrency = DEFAULT_THRESHOLD_MS_FOR_CONCURRENCY;

	private static boolean dump = false;

	private static boolean anonymize = false;

	private static String tagsDirectory = "/tags/";

	// use the newer diff. will be overridden if this is not possible. 
	private static boolean useLegacyDiff = false;

	private static ISvnProcessor processor;

    private static boolean useSvnKit = false;

	/**
	 * A utility class (only static methods) should be final and have a private
	 * constructor.
	 */
	private SvnConfigurationOptions() {
	}

	/**
	 * Returns the cacheDir.
	 * 
	 * @return String output Directory
	 */
	public static String getCacheDir() {
		return cacheDir;
	}

	/**
	 * Sets the cacheDir.
	 * 
	 * @param cacheDir
	 *            The cacheDir to set
	 * @throws ConfigurationException
	 *             if the cache directory cannot be created
	 */
	public static void setCacheDir(String cacheDir) throws ConfigurationException {
		if (!cacheDir.endsWith(FileUtils.getDirSeparator())) {
			cacheDir += FileUtils.getDefaultDirSeparator();
		}
		final File cDir = new File(cacheDir);
		if (!cDir.exists() && !cDir.mkdirs()) {
			throw new ConfigurationException("Can't create cache directory: " + cacheDir);
		}
		SvnConfigurationOptions.cacheDir = cacheDir;
	}

	/**
	 * Sets the cacheDir to the DEFAULT_CACHE_DIR
	 * 
	 * @throws ConfigurationException
	 *             if the cache directory cannot be created
	 */
	public static void setCacheDirToDefault() throws ConfigurationException {
		setCacheDir(DEFAULT_CACHE_DIR);
	}

	public static File getCheckedOutDirectoryAsFile() {
		return new File(FileUtils.getPathWithoutEndingSlash(ConfigurationOptions.getCheckedOutDirectory()) + FileUtils.getDirSeparator());
	}

	/**
	 * @return Returns the svnPassword.
	 */
	public static String getSvnPassword() {
		return svnPassword;
	}

	/**
	 * @param svnPassword
	 *            The svnPassword to set.
	 */
	public static void setSvnPassword(final String svnPassword) {
		SvnConfigurationOptions.svnPassword = svnPassword;
	}

	/**
	 * @return Returns the svnUsername.
	 */
	public static String getSvnUsername() {
		return svnUsername;
	}

	/**
	 * @param svnUsername
	 *            The svnUsername to set.
	 */
	public static void setSvnUsername(final String svnUsername) {
		SvnConfigurationOptions.svnUsername = svnUsername;
	}

	/**
	 * @return the taskLogger
	 */
	public static TaskLogger getTaskLogger() {
		return taskLogger;
	}

	/**
	 * @param taskLogger
	 *            the taskLogger to set
	 */
	public static void setTaskLogger(final TaskLogger taskLogger) {
		SvnConfigurationOptions.taskLogger = taskLogger;
	}

	/**
	 * @return the numberSvnDiffThreads
	 */
	public static int getNumberSvnDiffThreads() {
		return numberSvnDiffThreads;
	}

	/**
	 * @param numberSvnDiffThreads
	 *            the numberSvnDiffThreads to set
	 */
	public static void setNumberSvnDiffThreads(final int numberSvnDiffThreads) {
		SvnConfigurationOptions.numberSvnDiffThreads = numberSvnDiffThreads;
	}

	/**
	 * @return the thresholdInMsToUseConcurrency
	 */
	public static long getThresholdInMsToUseConcurrency() {
		return thresholdInMsToUseConcurrency;
	}

	/**
	 * @param thresholdInMsToUseConcurrency
	 *            the thresholdInMsToUseConcurrency to set
	 */
	public static void setThresholdInMsToUseConcurrency(final long thresholdToUseConcurrency) {
		SvnConfigurationOptions.thresholdInMsToUseConcurrency = thresholdToUseConcurrency;
	}

	public static void setDumpContent(final boolean dumpContent) {
		dump = dumpContent;
	}

	public static boolean isDumpContent() {
		return dump;
	}

	public static void setAnonymize(final boolean bAnon) {
		anonymize = bAnon;
	}

	public static boolean isAnonymize() {
		return anonymize;
	}

	/**
	 * Following request 1692245, add option -tags-dir to the command line.
	 */
	public static void setTagsDirectory(final String tagsDir) {
		if (tagsDir != null) {
			tagsDirectory = tagsDir.replace('\\', '/');
			if (!tagsDirectory.endsWith("/")) {
				tagsDirectory = tagsDir + "/";
			}
		}
	}

	/**
	 * Following request 1692245, add option -tags-dir to the command line.
	 */
	public static String getTagsDirectory() {
		return tagsDirectory;
	}

	/**
	 * Should we use a one diff per-file-per-revision or should we use the newer one diff per-revision?
	 * 
	 * @return true if legacy diff should be used. 
	 */
	public static boolean isLegacyDiff() {
		return useLegacyDiff;
	}

	/**
	 * Should we use a one diff per-file-per-revision or should we use the newer one diff per-revision?
	 * 
	 * @param isLegacy true if the legacy diff should be used.  
	 */
	public static void setLegacyDiff(final boolean isLegacy) {
		useLegacyDiff = isLegacy;
	}
	
	 /**
     * Should we use svnkit to query the repository
     * 
     * @return true if we should be using SVN kit.  
     */
    public static boolean isUsingSVNKit() {
        return useSvnKit;
    }

    /**
     * Should we use svnkit to query the repository. 
     * 
     * @param isSvnKit true if we should use svnkit  
     */
    public static void setUsingSvnKit(final boolean isSvnKit) {
        useSvnKit = isSvnKit;
    }

	public static ISvnProcessor getProcessor()
	{
	    if (processor==null) {
	        if (isUsingSVNKit()) {
	            try {
	            processor = new SvnKitProcessor();
	            } catch (NoClassDefFoundError ex)
	            {
	                getTaskLogger().error("Unable to find svnkit.jar and/or jna.jar in the same folder as statsvn.jar. Please copy these files and try again.");
	                throw ex;
	            }
	        }
	        else
	            processor = new SvnCommandLineProcessor();
	    }
	    return processor;
	        
	}

}
