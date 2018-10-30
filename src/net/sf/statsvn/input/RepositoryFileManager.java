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
 
 $RCSfile: RepositoryFileManager.java,v $ 
 Created on $Date: 2004/12/14 13:38:13 $ 
 */
package net.sf.statsvn.input;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import net.sf.statcvs.input.LogSyntaxException;
import net.sf.statcvs.input.NoLineCountException;
import net.sf.statcvs.util.FileUtils;
import net.sf.statsvn.output.SvnConfigurationOptions;
import net.sf.statsvn.util.BinaryDiffException;
import net.sf.statsvn.util.ISvnDiffProcessor;
import net.sf.statsvn.util.ISvnInfoProcessor;
import net.sf.statsvn.util.ISvnProcessor;
import net.sf.statsvn.util.ISvnPropgetProcessor;
import net.sf.statsvn.util.ISvnVersionProcessor;

/**
 * Manages a checked-out repository and provides access to line number counts
 * for repository files.
 * 
 * New in StatSVN: Also provides a central point of access to abstract out calls
 * to the server. Many of the methods here simply redirect to the static
 * util/SvnXXXUtils classes. Therefore, clients don't have to know where the
 * information is located, they can simply invoke this class.
 * 
 * @author Manuel Schulze
 * @author Steffen Pingel
 * @author Jason Kealey <jkealey@shade.ca>
 * 
 * @version $Id: RepositoryFileManager.java 394 2009-08-10 20:08:46Z jkealey $
 */
public class RepositoryFileManager {
	private final String path;

	/**
	 * Creates a new instance with root at <code>pathName</code>.
	 * 
	 * @param pathName
	 *            the root of the checked out repository
	 */
	public RepositoryFileManager(final String pathName) {
		path = pathName;
	}

	/**
	 * Converts an absolute path in the repository to a URL, using the
	 * repository URL
	 * 
	 * @param absolute
	 *            Example: /trunk/statsvn/package.html
	 * @return Example: svn://svn.statsvn.org/statsvn/trunk/statsvn/package.html
	 */
	public String absolutePathToUrl(final String absolute) {
		return getInfoProcessor().absolutePathToUrl(absolute);
	}

	/**
	 * Converts an absolute path in the repository to a path relative to the
	 * working folder root.
	 * 
	 * Will return null if absolute path does not start with getModuleName();
	 * 
	 * @param absolute
	 *            Example (assume getModuleName() returns /trunk/statsvn)
	 *            /trunk/statsvn/package.html
	 * @return Example: package.html
	 */
	public String absoluteToRelativePath(final String stringData) {
		return getInfoProcessor().absoluteToRelativePath(stringData);
	}

	/**
	 * Adds a directory to the list of known directories. Used when inferring
	 * implicit actions on deleted paths.
	 * 
	 * @param relativePath
	 *            the relative path.
	 */
	public void addDirectory(final String relativePath) {
		getInfoProcessor().addDirectory(relativePath);
	}

	/**
	 * Returns true if the file exists in the working copy (according to the svn
	 * metadata, and not file system checks).
	 * 
	 * @param relativePath
	 *            the path
	 * @return <tt>true</tt> if it exists
	 */
	public boolean existsInWorkingCopy(final String relativePath) {
		return getInfoProcessor().existsInWorkingCopy(relativePath);
	}

	/**
	 * Counts lines on a BufferedReader
	 * 
	 * @param reader
	 *            the buffered reader
	 * @return the number of lines read
	 * @throws IOException
	 *             error reading from reader
	 */
	protected int getLineCount(final BufferedReader reader) throws IOException {
		int linecount = 0;
		while (reader.readLine() != null) {
			linecount++;
		}
		return linecount;
	}

	/**
	 * Returns line count differences between two revisions of a file.
	 * 
	 * @param oldRevNr
	 *            old revision number
	 * @param newRevNr
	 *            new revision number
	 * @param filename
	 *            the filename
	 * @return A int[2] array of [lines added, lines removed] is returned.
	 * @throws IOException
	 *             problem parsing the stream
	 * @throws BinaryDiffException
	 *             if the error message is due to trying to diff binary files.
	 * 
	 */
	public int[] getLineDiff(final String oldRevNr, final String newRevNr, final String filename) throws IOException, BinaryDiffException {
	    
		return getDiffProcessor().getLineDiff(oldRevNr, newRevNr, filename);
	}

	/**
	* Returns line count differences for all files in a particular revision.
	* 
	* @param newRevNr
	*            new revision number
	* @return A vector of object[3] array of [filename, int[2](lines added, lines removed), isBinary] is returned.
	* @throws IOException
	*             problem parsing the stream
	* @throws BinaryDiffException
	*             if the error message is due to trying to diff binary files.
	*/
	public Vector getRevisionDiff(final String newRevNr) throws IOException, BinaryDiffException {
		return getDiffProcessor().getLineDiff(newRevNr);
	}

	/**
	 * Returns the lines of code for a repository file. (Currently checked out
	 * version)
	 * 
	 * @param filename
	 *            a file in the repository
	 * @return the lines of code for a repository file
	 * @throws NoLineCountException
	 *             when the line count could not be retrieved, for example when
	 *             the file was not found.
	 */
	public int getLinesOfCode(final String filename) throws NoLineCountException {
		final String absoluteName = FileUtils.getAbsoluteName(this.path, filename);
		try {
			final FileReader freader = new FileReader(absoluteName);
			final BufferedReader reader = new BufferedReader(freader);
			final int linecount = getLineCount(reader);
			SvnConfigurationOptions.getTaskLogger().log("line count for '" + absoluteName + "': " + linecount);
			freader.close();
			return linecount;
		} catch (final IOException e) {
			throw new NoLineCountException("could not get line count for '" + absoluteName + "': " + e);
		}
	}

	/**
	 * Assumes #loadInfo(String) has been called. Never ends with /, might be
	 * empty.
	 * 
	 * @return The absolute path of the root of the working folder in the
	 *         repository.
	 */
	public String getModuleName() {
		return getInfoProcessor().getModuleName();
	}

	/**
	 * Assumes #loadInfo(String) has been called.
	 * 
	 * @return The uuid of the repository.
	 */
	public String getRepositoryUuid() {
		return getInfoProcessor().getRepositoryUuid();
	}

	/**
	 * Returns the revision of filename in the local working directory by
	 * reading the svn metadata.
	 * 
	 * @param filename
	 *            the filename
	 * @return the revision of filename
	 */
	public String getRevision(final String filename) throws IOException {
		final String rev = getInfoProcessor().getRevisionNumber(filename);
		if (rev != null) {
			return rev;
		} else if (getInfoProcessor().isDirectory(filename)) {
			return null;
		} else {
			throw new IOException("File " + filename + " has no revision");
		}
	}

	/**
	 * Assumes #loadInfo(String) has been called.
	 *  
	 * @return the revision number of the root of the working folder 
	 *         (last checked out revision number)
	 */
	public String getRootRevisionNumber() {
		return getInfoProcessor().getRootRevisionNumber();
	}

	/**
	 * Is the given path a binary file in the <b>working</b> directory?
	 * 
	 * @param relativePath
	 *            the directory
	 * @return true if it is marked as a binary file
	 */
	public boolean isBinary(final String relativePath) {
		return getPropgetProcessor().getBinaryFiles().contains(relativePath);
	}

	/**
	 * Returns true if the path has been identified as a directory.
	 * 
	 * @param relativePath
	 *            the path
	 * @return true if it is a known directory.
	 */
	public boolean isDirectory(final String relativePath) {
		return getInfoProcessor().isDirectory(relativePath);
	}

	/**
	 * Initializes our representation of the repository.
	 * 
	 * @throws LogSyntaxException
	 *             if the svn info --xml is malformed
	 * @throws IOException
	 *             if there is an error reading from the stream
	 */
	public void loadInfo() throws LogSyntaxException, IOException {
		getInfoProcessor().loadInfo();
	}

	/**
	 * Converts a relative path in the working folder to a URL, using the
	 * working folder's root URL
	 * 
	 * @param relative
	 *            Example: src/Messages.java
	 * @return Example:
	 *         svn://svn.statsvn.org/statsvn/trunk/statsvn/src/Messages.java
	 * 
	 */
	public String relativePathToUrl(final String relative) {
		return getInfoProcessor().relativePathToUrl(relative);
	}

	/**
	 * Converts a relative path in the working folder to an absolute path in the
	 * repository.
	 * 
	 * @param relative
	 *            Example: src/Messages.java
	 * @return Example: /trunk/statsvn/src/Messages.java
	 * 
	 */
	public String relativeToAbsolutePath(final String relative) {
		return getInfoProcessor().relativeToAbsolutePath(relative);
	}

	/**
	 * Converts a url to an absolute path in the repository.
	 * 
	 * @param url
	 *            Examples: svn://svn.statsvn.org/statsvn/trunk/statsvn,
	 *            svn://svn.statsvn.org/statsvn/trunk/statsvn/package.html
	 * @return Example: /trunk/statsvn, /trunk/statsvn/package.html
	 */
	public String urlToAbsolutePath(final String url) {
		return getInfoProcessor().urlToAbsolutePath(url);
	}

	/**
	 * Converts a url to a relative path in the repository.
	 * 
	 * @param url
	 *            Examples: svn://svn.statsvn.org/statsvn/trunk/statsvn,
	 *            svn://svn.statsvn.org/statsvn/trunk/statsvn/package.html
	 * @return Example: ".", package.html
	 */
	public String urlToRelativePath(final String url) {
		return getInfoProcessor().urlToRelativePath(url);
	}
	
	private ISvnProcessor svnProcessor;
	public ISvnProcessor getProcessor()
	{
	    if (svnProcessor==null) svnProcessor = SvnConfigurationOptions.getProcessor();
	    return svnProcessor;
	}
	
	protected ISvnDiffProcessor getDiffProcessor()
    {
        return getProcessor().getDiffProcessor();
    }
	
    protected ISvnInfoProcessor getInfoProcessor()
    {
        return getProcessor().getInfoProcessor();
    }	
    protected ISvnPropgetProcessor getPropgetProcessor()
    {
        return getProcessor().getPropgetProcessor();
    }	
    protected ISvnVersionProcessor getVersionProcessor()
    {
        return getProcessor().getVersionProcessor();
    }
}
