package net.sf.statsvn.util.svnkit;

import java.io.File;
import java.io.IOException;

import net.sf.statcvs.input.LogSyntaxException;
import net.sf.statsvn.output.SvnConfigurationOptions;
import net.sf.statsvn.util.ISvnProcessor;
import net.sf.statsvn.util.SvnInfoUtils;
import net.sf.statsvn.util.SvnVersionMismatchException;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.xml.SVNXMLInfoHandler;
import org.xml.sax.Attributes;

/**
 * 
 * Performs svn info using svnkit. 
 * 
 * @author jkealey, yogesh
 *
 */
public class SvnKitInfo extends SvnInfoUtils {

    protected static class SvnKitInfoHandler extends SvnInfoUtils.SvnInfoHandler {

        public SvnKitInfoHandler(SvnInfoUtils infoUtils) {
            super(infoUtils);
        }

        protected boolean isRootFolder(Attributes attributes) {
            String path = attributes.getValue("path");
            // . is never returned by SvnKit, it appears. 
            return (path.equals(".") || new File(path).equals(((SvnKitInfo) getInfoUtils()).getCheckoutDirectory()))
                    && attributes.getValue("kind").equals("dir");
        }

    }

    public SvnKitInfo(ISvnProcessor processor) {
        super(processor);
    }

    /**
     * Verifies that the "svn info" command can return the repository root
     * (info available in svn >= 1.3.0)
     * 
     * @throws SvnVersionMismatchException
     *             if <tt>svn info</tt> failed to provide a non-empty repository root
     */
    public synchronized void checkRepoRootAvailable() throws SvnVersionMismatchException {

        try {
            loadInfo(true);
            if (getRootUrl() != null)
                return;
        } catch (Exception e) {
            SvnConfigurationOptions.getTaskLogger().info(e.getMessage());
        }

        throw new SvnVersionMismatchException(SVN_REPO_ROOT_NOTFOUND);
    }

    public File getCheckoutDirectory() {
        return getSvnKitProcessor().getCheckoutDirectory();
    }

    public SVNClientManager getManager() {
        return getSvnKitProcessor().getManager();
    }

    public SvnKitProcessor getSvnKitProcessor() {
        return (SvnKitProcessor) getProcessor();
    }

    protected void handleSvnException(SVNException ex) throws IOException {
        String msg = "svn info " + ex.getMessage();
        SvnConfigurationOptions.getTaskLogger().error(msg);
        throw new IOException(msg);
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
        if (isQueryNeeded(true /*bRootOnly*/)) {
            clearCache();

            try {
                SVNXMLInfoHandler handler = new SVNXMLInfoHandler(new SvnKitInfoHandler(this));
                handler.setTargetPath(getCheckoutDirectory());
                getManager().getWCClient().doInfo(getCheckoutDirectory(), null, null, SVNDepth.fromRecurse(!bRootOnly), null,
                        handler);
            } catch (SVNException e) {
                handleSvnException(e);
            }
        }
    }

}
