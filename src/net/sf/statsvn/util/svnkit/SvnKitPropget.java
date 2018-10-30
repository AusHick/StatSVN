package net.sf.statsvn.util.svnkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.statsvn.output.SvnConfigurationOptions;
import net.sf.statsvn.util.ISvnProcessor;
import net.sf.statsvn.util.SvnPropgetUtils;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNPropertyHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * 
 * Uses svnkit to do svn propget. 
 * @author jkealey, yogesh
 *
 */
public class SvnKitPropget extends SvnPropgetUtils {

    protected class SvnKitPropertyHandler implements ISVNPropertyHandler {

        protected List binaryFiles;
        SvnKitPropget propgetUtils;

        public SvnKitPropertyHandler(SvnKitPropget propgetUtils, List binaryFiles) {
            this.binaryFiles = binaryFiles;
            this.propgetUtils = propgetUtils;
        }

        protected List getBinaryFiles() {
            return binaryFiles;
        }

        protected SvnKitPropget getPropgetUtils() {
            return propgetUtils;
        }

        public void handleProperty(File file, SVNPropertyData data) throws SVNException {
            if (isBinary(data)) {
                String relativePath = file.getAbsoluteFile().getAbsolutePath().substring(
                        getPropgetUtils().getCheckoutDirectory().getAbsoluteFile().getAbsolutePath().length()+1);
                binaryFiles.add(relativePath.replace(File.separatorChar, '/'));
            }
        }

        public void handleProperty(long revision, SVNPropertyData data) throws SVNException {
           // System.out.println(data.getValue());
        }

        public void handleProperty(SVNURL url, SVNPropertyData data) throws SVNException {
            if (getPropgetUtils().isBinary(data)) {
                String path = getPropgetUtils().getProcessor().getInfoProcessor().urlToRelativePath(url.toString());
                //System.out.println(path);
                binaryFiles.add(path.replace(File.separatorChar, '/'));
            }
        }

    }

    public SvnKitPropget(ISvnProcessor processor) {
        super(processor);
    }

    public List getBinaryFiles() {
        if (binaryFiles == null) {

            binaryFiles = new ArrayList();
            try {
                
                getManager().getWCClient().doGetProperty(getCheckoutDirectory(), SVNProperty.MIME_TYPE, SVNRevision.WORKING, SVNRevision.WORKING,
                        SVNDepth.INFINITY, new SvnKitPropertyHandler(this, binaryFiles), null);
            } catch (SVNException e) {
                try {
                    handleSvnException(e);
                } catch (IOException ex) {
                }
            }
        }

        return binaryFiles;
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
        String msg = "svn propget " + ex.getMessage();
        SvnConfigurationOptions.getTaskLogger().error(msg);
        throw new IOException(msg);
    }

    protected boolean isBinary(SVNPropertyData data) {
        return data != null && (data.getValue().toString().equals("application/octet-stream") || data.getValue().toString().indexOf("text/") < 0);
    }
    
    public boolean isBinaryFile(final String revision, final String filename) {
        try {
            // TODO: HAS NEVER BEEN TESTED. 
            SVNPropertyData data = getManager().getWCClient().doGetProperty(new File(filename), SVNProperty.MIME_TYPE, SVNRevision.parse(revision),
                    SVNRevision.parse(revision));
            return isBinary(data);
        } catch (SVNException e) {
            try {
                handleSvnException(e);
            } catch (IOException ex) {
            }
            return false;
        }
    }
}

