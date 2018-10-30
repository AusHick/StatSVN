package net.sf.statsvn.util;

import java.io.IOException;
import java.io.InputStream;

import net.sf.statcvs.input.LogSyntaxException;

/**
 * Performs svn info queries.
 *  
 * @author jkealey
 *
 */
public interface ISvnInfoProcessor {

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
    public abstract String absoluteToRelativePath(String absolute);

    /**
     * Converts an absolute path in the repository to a URL, using the
     * repository URL
     * 
     * @param absolute
     *            Example: /trunk/statsvn/package.html
     * @return Example: svn://svn.statsvn.org/statsvn/trunk/statsvn/package.html
     */
    public abstract String absolutePathToUrl(final String absolute);

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
    public abstract String relativePathToUrl(String relative);

    /**
     * Converts a relative path in the working folder to an absolute path in the
     * repository.
     * 
     * @param relative
     *            Example: src/Messages.java
     * @return Example: /trunk/statsvn/src/Messages.java
     * 
     */
    public abstract String relativeToAbsolutePath(final String relative);

    /**
     * Returns true if the file exists in the working copy (according to the svn
     * metadata, and not file system checks).
     * 
     * @param relativePath
     *            the path
     * @return <tt>true</tt> if it exists
     */
    public abstract boolean existsInWorkingCopy(final String relativePath);

    /**
     * Assumes #loadInfo(String) has been called. Never ends with /, might be
     * empty.
     * 
     * @return The absolute path of the root of the working folder in the
     *         repository.
     */
    public abstract String getModuleName();

    /**
     * Returns the revision number of the file in the working copy.
     * 
     * @param relativePath
     *            the filename
     * @return the revision number if it exists in the working copy, null
     *         otherwise.
     */
    public abstract String getRevisionNumber(final String relativePath);

    /**
     * Assumes #loadInfo() has been invoked.
     * 
     * @return the root of the working folder's revision number (last checked
     *         out revision number)
     */
    public abstract String getRootRevisionNumber();

    /**
     * Assumes #loadInfo() has been invoked.
     * 
     * @return the root of the working folder's url (example:
     *         svn://svn.statsvn.org/statsvn/trunk/statsvn)
     */
    public abstract String getRootUrl();

    /**
     * Assumes #loadInfo() has been invoked.
     * 
     * @return the uuid of the repository
     */
    public abstract String getRepositoryUuid();

    /**
     * Assumes #loadInfo() has been invoked.
     * 
     * @return the repository url (example: svn://svn.statsvn.org/statsvn)
     */
    public abstract String getRepositoryUrl();

    /**
     * Returns true if the path has been identified as a directory.
     * 
     * @param relativePath
     *            the path
     * @return true if it is a known directory.
     */
    public abstract boolean isDirectory(final String relativePath);

    /**
     * Adds a directory to the list of known directories. Used when inferring
     * implicit actions on deleted paths.
     * 
     * @param relativePath
     *            the relative path.
     */
    public abstract void addDirectory(final String relativePath);

    /**
     * Loads the information from svn info if needed.
     * 
     * @param stream
     *            the input stream representing 
     *            an svn info command.
     * @throws LogSyntaxException
     *             if the format of the svn info is invalid
     * @throws IOException
     *             if we can't read from the response stream.
     */
    public abstract void loadInfo(final InputStream stream) throws LogSyntaxException, IOException;

    /**
     * Initializes our representation of the repository.
     * 
     * @throws LogSyntaxException
     *             if the svn info --xml is malformed
     * @throws IOException
     *             if there is an error reading from the stream
     */
    public abstract void loadInfo() throws LogSyntaxException, IOException;

    /**
     * Converts a url to an absolute path in the repository.
     * 
     * @param url
     *            Examples: svn://svn.statsvn.org/statsvn/trunk/statsvn,
     *            svn://svn.statsvn.org/statsvn/trunk/statsvn/package.html
     * @return Example: /trunk/statsvn, /trunk/statsvn/package.html
     */
    public abstract String urlToAbsolutePath(String url);

    /**
     * Converts a url to a relative path in the repository.
     * 
     * @param url
     *            Examples: svn://svn.statsvn.org/statsvn/trunk/statsvn,
     *            svn://svn.statsvn.org/statsvn/trunk/statsvn/package.html
     * @return Example: ".", package.html
     */
    public abstract String urlToRelativePath(final String url);

    
    /**
     * Verifies that the "svn info" command can return the repository root
     * (info available in svn >= 1.3.0)
     * 
     * @throws SvnVersionMismatchException
     *             if <tt>svn info</tt> failed to provide a non-empty repository root
     */
    public abstract void checkRepoRootAvailable() throws SvnVersionMismatchException ;
}