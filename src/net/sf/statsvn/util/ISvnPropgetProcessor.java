package net.sf.statsvn.util;

import java.io.IOException;
import java.util.List;

/**
 * 
 * Performs svn propget queries. 
 * 
 * @author jkealey
 *
 */
public interface ISvnPropgetProcessor {

    /**
     * Returns the list of binary files in the working directory.
     * 
     * @return the list of binary files
     */
    public abstract List getBinaryFiles();

    /**
     * It was first thought that a the mime-type of a file's previous revision
     * could be found. This is not the case. Leave revision null until future
     * upgrade of svn propget command line.
     * 
     * @param revision
     *            the revision to query
     * @param filename
     *            the filename
     * @return if that version of a file is binary
     */
    public abstract boolean isBinaryFile(final String revision, final String filename);

    
    /**
     * Loads the list of binary files from the input stream equivalent to an svn
     * propget command.
     * 
     * @param path
     *            a file on disk which contains the results of an svn propget
     */ 
    public void loadBinaryFiles(final String path) throws IOException;
}