package net.sf.statsvn.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.sf.statcvs.util.LookaheadReader;
import net.sf.statsvn.output.SvnConfigurationOptions;

/**
 * Utilities class that manages calls to svn propget. Used to find binary files.
 * 
 * @author Jason Kealey <jkealey@shade.ca>
 * 
 * @version $Id: SvnPropgetUtils.java 394 2009-08-10 20:08:46Z jkealey $
 */
public  class SvnPropgetUtils implements ISvnPropgetProcessor {

	protected List binaryFiles;

    protected ISvnProcessor processor;

    /**
     * Invokes propget via the svn propget via the command line.  
     */
    public SvnPropgetUtils(ISvnProcessor processor) {
        this.processor = processor;
    }

    protected ISvnProcessor getProcessor() {
        return processor;
    }

	/**
	 * Get the svn:mime-types for all files, latest revision.
	 * 
	 * @return the inputstream from which to read the information.
	 */
	protected synchronized ProcessUtils getFileMimeTypes() {
		return getFileMimeTypes(null, null);
	}

	/**
	 * Get the svn:mime-type for a certain file (leave null for all files).
	 * 
	 * @param revision
	 *            revision for which to query;
	 * @param filename
	 *            the filename (or null for all files)
	 * @return the inputstream from which to read the information.
	 */
	protected synchronized ProcessUtils getFileMimeTypes(final String revision, final String filename) {
		String svnPropgetCommand = "svn propget svn:mime-type";
		if (revision != null && revision.length() > 0) {
			svnPropgetCommand += " -r " + revision;
		}

		if (filename != null && filename.length() > 0) {
			svnPropgetCommand += " " + StringUtils.replace(" ", "%20", getProcessor().getInfoProcessor().relativePathToUrl(filename));

			if (revision != null && revision.length() > 0) {
				svnPropgetCommand += "@" + revision;
			}
		} else {
			svnPropgetCommand += " -R ";
		}

		svnPropgetCommand += SvnCommandHelper.getAuthString();

		try {
			return ProcessUtils.call(svnPropgetCommand);
		} catch (final Exception e) {
			SvnConfigurationOptions.getTaskLogger().info(e.toString());
			return null;
		}
	}

	/* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnPropgetProcessor#getBinaryFiles()
     */
	public List getBinaryFiles() {
		if (binaryFiles == null) {
			ProcessUtils pUtils = null;
			try {
				pUtils = getFileMimeTypes();
				loadBinaryFiles(pUtils);
			} finally {
				if (pUtils != null) {
					try {
						pUtils.close();
					} catch (final IOException e) {
						SvnConfigurationOptions.getTaskLogger().info(e.toString());
					}
				}
			}
		}

		return binaryFiles;
	}
	
	
    /**
     * Loads the list of binary files from the input stream equivalent to an svn
     * propget command.
     * 
     * @param path
     *            a file on disk which contains the results of an svn propget
     */	
	public void loadBinaryFiles(final String path) throws IOException
	{
       final InputStream stream = new FileInputStream(path);
        final ProcessUtils pUtils = new ProcessUtils();
        try {
            pUtils.setInputStream(stream);
            loadBinaryFiles(pUtils);
        } finally {
            pUtils.close();
        }
	}
	/**
	 * Loads the list of binary files from the input stream equivalent to an svn
	 * propget command.
	 * 
	 * @param stream
	 *            stream equivalent to an svn propget command
	 */
	protected void loadBinaryFiles(final ProcessUtils pUtils) {
		binaryFiles = new ArrayList();
		final LookaheadReader mimeReader = new LookaheadReader(new InputStreamReader(pUtils.getInputStream()));
		try {
			while (mimeReader.hasNextLine()) {
				mimeReader.nextLine();
				final String file = getBinaryFilename(mimeReader.getCurrentLine(), false);
				if (file != null) {
					binaryFiles.add(file);
				}
			}
			if (pUtils.hasErrorOccured()) {
				throw new IOException(pUtils.getErrorMessage());
			}
		} catch (final IOException e) {
			SvnConfigurationOptions.getTaskLogger().info(e.getMessage());
		}
	}

	/* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnPropgetProcessor#isBinaryFile(java.lang.String, java.lang.String)
     */
	public boolean isBinaryFile(final String revision, final String filename) {
		ProcessUtils pUtils = null;
		try {
			pUtils = getFileMimeTypes(revision, filename);
			final LookaheadReader mimeReader = new LookaheadReader(new InputStreamReader(pUtils.getInputStream()));
			while (mimeReader.hasNextLine()) {
				mimeReader.nextLine();
				final String file = getBinaryFilename(mimeReader.getCurrentLine(), true);
				if (file != null && file.equals(filename)) {
					return true;
				}
			}
		} catch (final IOException e) {
			SvnConfigurationOptions.getTaskLogger().info(e.toString());
		} finally {
			if (pUtils != null) {
				try {
					pUtils.close();
				} catch (final IOException e) {
					SvnConfigurationOptions.getTaskLogger().info(e.toString());
				}
			}
		}

		return false;
	}

	/**
	 * Given a string such as: "lib\junit.jar - application/octet-stream" or
	 * "svn:\\host\repo\lib\junit.jar - application/octet-stream" will return
	 * the filename if the mime type is binary (doesn't end with text/*)
	 * 
	 * Will return the filename with / was a directory seperator.
	 * 
	 * @param currentLine
	 *            the line obtained from svn propget svn:mime-type
	 * @param removeRoot
	 *            if true, will remove any repository prefix
	 * @return should return lib\junit.jar in both cases, given that
	 *         removeRoot==true in the second case.
	 */
	protected String getBinaryFilename(final String currentLine, final boolean removeRoot) {
		// want to make sure we only have / in end result.
		String line = removeRoot ? currentLine : currentLine.replace('\\', '/');

		// HACK: See bug 18. if removeRoot==true, no - will be found because we
		// are calling for one specific file.
		final String octetStream = " - application/octet-stream";
		// if is common binary file or identified as something other than text
		if (line.endsWith(octetStream) || line.lastIndexOf(" - text/") < 0 && line.lastIndexOf(" - text/") == line.lastIndexOf(" - ")
		        && line.lastIndexOf(" - ") >= 0) {
			line = line.substring(0, line.lastIndexOf(" - "));
			if (removeRoot) {
				line = getProcessor().getInfoProcessor().urlToRelativePath(line);
			}
			return line;
		}

		return null;
	}

}
