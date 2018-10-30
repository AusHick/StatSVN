package net.sf.statsvn.util;

/**
 * Performs svn version queries. 
 * @author Administrator
 *
 */
public interface ISvnVersionProcessor {

    /**
     * Verifies that the current revision of SVN is SVN_MINIMUM_VERSION
     * 
     * @throws SvnVersionMismatchException
     *             if SVN executable not found or version less than
     *             SVN_MINIMUM_VERSION
     * @return the version string
     */
    public abstract String checkSvnVersionSufficient() throws SvnVersionMismatchException;

    /**
     * Verifies that the given version supports one diff per revision (version>=1.4.0)
     * 
     * @param version the current version
     * @return true if one can do an svn diff per revision
     */
    public abstract boolean checkDiffPerRevPossible(final String version);

}