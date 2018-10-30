package net.sf.statsvn.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.statcvs.input.LogSyntaxException;
import net.sf.statcvs.util.LookaheadReader;
import net.sf.statsvn.output.SvnConfigurationOptions;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Utilities class that manages calls to svn info. Used to find repository
 * information, latest revision numbers, and directories.
 * 
 * @author Jason Kealey <jkealey@shade.ca>
 * 
 * @version $Id: SvnInfoUtils.java 405 2010-02-23 14:29:15Z jkealey $
 */
public class SvnInfoUtils implements ISvnInfoProcessor {

    //  HACK: we "should" parse the output and check for a node named root, but this will work well enough
    private static final String SVN_INFO_WITHREPO_LINE_PATTERN = ".*<root>.+</root>.*";

    protected static final String SVN_REPO_ROOT_NOTFOUND = "Repository root not available - verify that the project was checked out with svn version "
            + SvnStartupUtils.SVN_MINIMUM_VERSION + " or above.";

    
    protected ISvnProcessor processor;

    /**
     * Invokes info using the svn info command line. 
     */
    public SvnInfoUtils(ISvnProcessor processor) {
        this.processor = processor;
    }

    protected ISvnProcessor getProcessor() {
        return processor;
    }

    /**
     * SAX parser for the svn info --xml command.
     * 
     * @author jkealey
     */
    protected static class SvnInfoHandler extends DefaultHandler {

        private boolean isRootFolder = false;
        private String sCurrentKind;
        private String sCurrentRevision;
        private String sCurrentUrl;
        private String stringData = "";
        private String sCurrentPath;
        private SvnInfoUtils infoUtils;

        public SvnInfoUtils getInfoUtils() {
            return infoUtils;
        }

        public SvnInfoHandler(SvnInfoUtils infoUtils) {
            this.infoUtils = infoUtils;
        }

        /**
         * Builds the string that was read; default implementation can invoke
         * this function multiple times while reading the data.
         */
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            stringData += new String(ch, start, length);
        }

        /**
         * End of xml element.
         */
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            String eName = localName; // element name
            if ("".equals(eName)) {
                eName = qName; // namespaceAware = false
            }

            if (isRootFolder && eName.equals("url")) {
                isRootFolder = false;
                getInfoUtils().setRootUrl(stringData);
                sCurrentUrl = stringData;
            } else if (eName.equals("url")) {
                sCurrentUrl = stringData;
            } else if (eName.equals("entry")) {
                if (sCurrentRevision == null || sCurrentUrl == null || sCurrentKind == null) {
                    throw new SAXException("Invalid svn info xml; unable to find revision or url for path [" + sCurrentPath + "]" + " revision="
                            + sCurrentRevision + " url:" + sCurrentUrl + " kind:" + sCurrentKind);
                }

                final String path = getInfoUtils().urlToRelativePath(sCurrentUrl);
                getInfoUtils().HM_REVISIONS.put(path, sCurrentRevision);
                if (sCurrentKind.equals("dir")) {
                    getInfoUtils().HS_DIRECTORIES.add(path);
                }
            } else if (eName.equals("uuid")) {
                getInfoUtils().setRepositoryUuid(stringData);
            } else if (eName.equals("root")) {
                getInfoUtils().setRepositoryUrl(stringData);
            }
        }

        /**
         * Start of XML element.
         */
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
            String eName = localName; // element name
            if ("".equals(eName)) {
                eName = qName; // namespaceAware = false
            }

            if (eName.equals("entry")) {
                sCurrentPath = attributes.getValue("path");
                if (!isValidInfoEntry(attributes)) {
                    throw new SAXException("Invalid svn info xml for entry element. Please verify that you have checked out this project using "
                            + "Subversion 1.3 or above, not only that you are currently using this version.");
                }

                if (getInfoUtils().getRootUrl() == null && isRootFolder(attributes)) {
                    isRootFolder = true;
                    getInfoUtils().sRootRevisionNumber = attributes.getValue("revision");
                }

                sCurrentRevision = null;
                sCurrentUrl = null;
                sCurrentKind = attributes.getValue("kind");
            } else if (eName.equals("commit")) {
                if (!isValidCommit(attributes)) {
                    throw new SAXException("Invalid svn info xml for commit element. Please verify that you have checked out this project using "
                            + "Subversion 1.3 or above, not only that you are currently using this version.");
                }
                sCurrentRevision = attributes.getValue("revision");
            }

            stringData = "";
        }

        /**
         * Is this the root of the workspace?
         * 
         * @param attributes
         *            the xml attributes
         * @return true if is the root folder.
         */
        protected  boolean isRootFolder(final Attributes attributes) {
            return attributes.getValue("path").equals(".") && attributes.getValue("kind").equals("dir");
        }

        /**
         * Is this a valid commit? Check to see if wec an read the revision
         * number.
         * 
         * @param attributes
         *            the xml attributes
         * @return true if is a valid commit.
         */
        protected static boolean isValidCommit(final Attributes attributes) {
            return attributes != null && attributes.getValue("revision") != null;
        }

        /**
         * Is this a valid info entry? Check to see if we can read path, kind
         * and revision.
         * 
         * @param attributes
         *            the xml attributes.
         * @return true if is a valid info entry.
         */
        protected static boolean isValidInfoEntry(final Attributes attributes) {
            return attributes != null && attributes.getValue("path") != null && attributes.getValue("kind") != null && attributes.getValue("revision") != null;
        }
    }

    // enable caching to speed up calculations
    private final boolean ENABLE_CACHING = true;

    // relative path -> Revision Number
    protected final HashMap HM_REVISIONS = new HashMap();

    // if HashSet contains relative path, path is a directory.
    protected final HashSet HS_DIRECTORIES = new HashSet();

    // Path of . in repository. Can only be calculated if given an element from
    // the SVN log.
    private String sModuleName = null;

    // Revision number of root folder (.)
    private String sRootRevisionNumber = null;

    // URL of root (.)
    private String sRootUrl = null;

    // UUID of repository
    private String sRepositoryUuid = null;

    // URL of repository
    private String sRepositoryUrl = null;

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#absoluteToRelativePath(java.lang.String)
     */
    public String absoluteToRelativePath(String absolute) {
        if (absolute.endsWith("/")) {
            absolute = absolute.substring(0, absolute.length() - 1);
        }

        if (absolute.equals(getModuleName())) {
            return ".";
        } else if (!absolute.startsWith(getModuleName())) {
            return null;
        } else {
            return absolute.substring(getModuleName().length() + 1);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#absolutePathToUrl(java.lang.String)
     */
    public String absolutePathToUrl(final String absolute) {
        return getRepositoryUrl() + (absolute.endsWith("/") ? absolute.substring(0, absolute.length() - 1) : absolute);
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#relativePathToUrl(java.lang.String)
     */
    public String relativePathToUrl(String relative) {
        relative = relative.replace('\\', '/');
        if (relative.equals(".") || relative.length() == 0) {
            return getRootUrl();
        } else {
            return getRootUrl() + "/" + (relative.endsWith("/") ? relative.substring(0, relative.length() - 1) : relative);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#relativeToAbsolutePath(java.lang.String)
     */
    public String relativeToAbsolutePath(final String relative) {
        return urlToAbsolutePath(relativePathToUrl(relative));
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#existsInWorkingCopy(java.lang.String)
     */
    public boolean existsInWorkingCopy(final String relativePath) {
        return getRevisionNumber(relativePath) != null;
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#getModuleName()
     */
    public String getModuleName() {

        if (sModuleName == null) {

            if (getRootUrl().length() < getRepositoryUrl().length() || getRepositoryUrl().length() == 0) {
                SvnConfigurationOptions.getTaskLogger().info("Unable to process module name.");
                sModuleName = "";
            } else {
                try {
                    sModuleName = URLDecoder.decode(getRootUrl().substring(getRepositoryUrl().length()), "UTF-8");
                } catch (final UnsupportedEncodingException e) {
                    SvnConfigurationOptions.getTaskLogger().error(e.toString());
                }
            }

        }
        return sModuleName;
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#getRevisionNumber(java.lang.String)
     */
    public String getRevisionNumber(final String relativePath) {
        if (HM_REVISIONS.containsKey(relativePath)) {
            return HM_REVISIONS.get(relativePath).toString();
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#getRootRevisionNumber()
     */
    public String getRootRevisionNumber() {
        return sRootRevisionNumber;
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#getRootUrl()
     */
    public String getRootUrl() {
        return sRootUrl;
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#getRepositoryUuid()
     */
    public String getRepositoryUuid() {
        return sRepositoryUuid;
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#getRepositoryUrl()
     */
    public String getRepositoryUrl() {
        return sRepositoryUrl;
    }

    /**
     * Invokes svn info.
     * 
     * @param bRootOnly
     *            true if should we check for the root only or false otherwise
     *            (recurse for all files)
     * @return the response.
     */
    protected synchronized ProcessUtils getSvnInfo(boolean bRootOnly) {
        String svnInfoCommand = "svn info --xml";
        if (!bRootOnly) {
            svnInfoCommand += " -R";
        }
        svnInfoCommand += SvnCommandHelper.getAuthString();

        try {
            return ProcessUtils.call(svnInfoCommand);
        } catch (final Exception e) {
            SvnConfigurationOptions.getTaskLogger().error(e.toString());
            return null;
        }
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#isDirectory(java.lang.String)
     */
    public boolean isDirectory(final String relativePath) {
        return HS_DIRECTORIES.contains(relativePath);
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#addDirectory(java.lang.String)
     */
    public void addDirectory(final String relativePath) {
        if (!HS_DIRECTORIES.contains(relativePath)) {
            HS_DIRECTORIES.add(relativePath);
        }
    }

    /**
     * Do we need to re-invoke svn info?
     * 
     * @param bRootOnly
     *            true if we need the root only
     * @return true if we it needs to be re-invoked.
     */
    protected boolean isQueryNeeded(boolean bRootOnly) {
        return !ENABLE_CACHING || (bRootOnly && sRootUrl == null) || (!bRootOnly && HM_REVISIONS == null);
    }

    /**
     * Loads the information from svn info if needed.
     * 
     * @param bRootOnly
     *            load only the root?
     * @throws LogSyntaxException
     *             if the format of the svn info is invalid
     * @throws IOException
     *             if we can't read from the response stream.
     */
    protected void loadInfo(final boolean bRootOnly) throws LogSyntaxException, IOException {
        ProcessUtils pUtils = null;
        try {
            pUtils = getSvnInfo(bRootOnly);
            loadInfo(pUtils.getInputStream());
            
            if (pUtils.hasErrorOccured()) {
                throw new IOException("svn info: " + pUtils.getErrorMessage());
            }
            
        } finally {
            if (pUtils != null) {
                pUtils.close();
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#loadInfo(net.sf.statsvn.util.ProcessUtils)
     */
    public void loadInfo(final InputStream stream) throws LogSyntaxException, IOException {
        if (isQueryNeeded(true)) {
            try {
                clearCache();

                final SAXParserFactory factory = SAXParserFactory.newInstance();
                final SAXParser parser = factory.newSAXParser();
                parser.parse(stream, new SvnInfoHandler(this));

            } catch (final ParserConfigurationException e) {
                throw new LogSyntaxException("svn info: " + e.getMessage());
            } catch (final SAXException e) {
                throw new LogSyntaxException("svn info: " + e.getMessage());
            }
        }
    }

    protected void clearCache() {
        HM_REVISIONS.clear();
        HS_DIRECTORIES.clear();
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#loadInfo()
     */
    public void loadInfo() throws LogSyntaxException, IOException {
        loadInfo(false);
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#urlToAbsolutePath(java.lang.String)
     */
    public String urlToAbsolutePath(String url) {
        String result = url;
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (getModuleName().length() <= 1) {
            if (getRootUrl().equals(url)) {
                result = "/";
            } else {
                result = url.substring(getRootUrl().length());
            }
        } else {
            // chop off the repo root from the url
            result = url.substring(getRepositoryUrl().length());
        }
        
        // bugs with spaces in filenames. 
        String decoded;
        try {
            decoded =  URLDecoder.decode(result, "UTF-8");
        } catch (UnsupportedEncodingException ex)
        {
            decoded = result;
        }
        
        return decoded;
    }

    /* (non-Javadoc)
     * @see net.sf.statsvn.util.ISvnInfoProcessor#urlToRelativePath(java.lang.String)
     */
    public String urlToRelativePath(final String url) {
        return absoluteToRelativePath(urlToAbsolutePath(url));
    }

    /**
     * Sets the project's root URL.
     * 
     * @param rootUrl
     */
    protected void setRootUrl(final String rootUrl) {
        if (rootUrl.endsWith("/")) {
            sRootUrl = rootUrl.substring(0, rootUrl.length() - 1);
        } else {
            sRootUrl = rootUrl;
        }

        sModuleName = null;
    }

    /**
     * Sets the project's repository URL.
     * 
     * @param repositoryUrl
     */
    protected void setRepositoryUrl(final String repositoryUrl) {
        if (repositoryUrl.endsWith("/")) {
            sRepositoryUrl = repositoryUrl.substring(0, repositoryUrl.length() - 1);
        } else {
            sRepositoryUrl = repositoryUrl;
        }

        sModuleName = null;
    }
    

    protected void setRepositoryUuid(String repositoryUuid) {
        sRepositoryUuid = repositoryUuid;
    }

    
    /**
     * Verifies that the "svn info" command can return the repository root
     * (info available in svn >= 1.3.0)
     * 
     * @throws SvnVersionMismatchException
     *             if <tt>svn info</tt> failed to provide a non-empty repository root
     */
    public synchronized void checkRepoRootAvailable() throws SvnVersionMismatchException {
        ProcessUtils pUtils = null;
        try {
            final boolean rootOnlyTrue = true;
            pUtils = getSvnInfo(rootOnlyTrue);
            final InputStream istream = pUtils.getInputStream();
            final LookaheadReader reader = new LookaheadReader(new InputStreamReader(istream));

            while (reader.hasNextLine()) {
                final String line = reader.nextLine();
                if (line.matches(SVN_INFO_WITHREPO_LINE_PATTERN)) {
                    // We have our <root> element in the svn info AND it's not empty --> checkout performed 
                    // with a compatible version of subversion client.
                    istream.close();
                    return; // success
                }
            }

            if (pUtils.hasErrorOccured()) {
                throw new IOException(pUtils.getErrorMessage());
            }
        } catch (final Exception e) {
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

        throw new SvnVersionMismatchException(SVN_REPO_ROOT_NOTFOUND);
    }    
}
