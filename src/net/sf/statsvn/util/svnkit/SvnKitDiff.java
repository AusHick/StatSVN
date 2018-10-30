package net.sf.statsvn.util.svnkit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import net.sf.statsvn.output.SvnConfigurationOptions;
import net.sf.statsvn.util.BinaryDiffException;
import net.sf.statsvn.util.StringUtils;
import net.sf.statsvn.util.SvnDiffUtils;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 *
 * Performs diffs via svnkit. 
 * 
 * @author jkealey, yogesh
 *
 */
public class SvnKitDiff extends SvnDiffUtils {

    /**
     * This method converts absolute paths inside the diff output to relative ones. 
     */
    private static String replaceRelativePathWithinDiffData(File rootDirectory, String diffData) {
        String rootPath = rootDirectory.getAbsoluteFile().getAbsolutePath();
        //        rootPath =  rootPath.replace(File.separator, "/") + "/"; // removing dependency to jdk1.5
        rootPath = StringUtils.replace(File.separator, "/", rootPath);
        // return diffData.replace(rootPath, "");  // removing dependency to jdk1.5
        return StringUtils.replace(rootPath, "", diffData);
    }

    /**
     * Performs diffs via svnkit. 
     * 
     * @param processor the base processor  
     */
    public SvnKitDiff(SvnKitProcessor processor) {
        super(processor);
    }

    /**
     * Shorthand to get the checked out directory
     * @return the checked out directory 
     */
    public File getCheckoutDirectory() {
        return getSvnKitProcessor().getCheckoutDirectory();
    }

    /**
     * Gets diffs inside one revision. 
     * 
     * @return a list of diffs that were extracted from one particular revision    
     */
    public Vector getLineDiff(String newRevNr) throws IOException, BinaryDiffException {
        ByteArrayOutputStream diffBytes = new ByteArrayOutputStream();
        int revisionNo = Integer.parseInt(newRevNr);
        try {
            getManager().getDiffClient().doDiff(getCheckoutDirectory(), SVNRevision.create(revisionNo), SVNRevision.create(revisionNo - 1),
                    SVNRevision.create(revisionNo), SVNDepth.INFINITY, false, diffBytes, null);
        } catch (SVNException ex) {
            handleSvnException(ex);
        }
        String modDiffDataStr = replaceRelativePathWithinDiffData(getCheckoutDirectory(), diffBytes.toString());

        final Vector answer = new Vector();
        parseMultipleDiffStream(answer, new ByteArrayInputStream(modDiffDataStr.getBytes()));
        return answer;
    }

    /**
     * Gets a single diff for a file between two revisions. 
     */
    public int[] getLineDiff(String oldRevNr, String newRevNr, String filename) throws IOException, BinaryDiffException {

        int oldRevisionNo = Integer.parseInt(oldRevNr);
        int newRevisionNo = Integer.parseInt(newRevNr);
        File newFile = new File(getProcessor().getInfoProcessor().relativeToAbsolutePath(filename));
        File oldFile = newFile;
        ByteArrayOutputStream diffBytes = new ByteArrayOutputStream();
        try {
            getManager().getDiffClient().doDiff(oldFile, SVNRevision.create(oldRevisionNo), newFile, SVNRevision.create(newRevisionNo), SVNDepth.INFINITY,
                    false, diffBytes, null);
        } catch (SVNException ex) {
            handleSvnException(ex);
        }
        String modDiffDataStr = replaceRelativePathWithinDiffData(getCheckoutDirectory(), diffBytes.toString());

        return parseSingleDiffStream(new ByteArrayInputStream(modDiffDataStr.getBytes()));
    }

    /**
     * Shorthand for the svnkit client manager. 
     * 
     * @return the svnkit client manager
     */
    public SVNClientManager getManager() {
        return getSvnKitProcessor().getManager();
    }

    /**
     * Shorthand to get the base processor 
     * @return the base processor 
     */
    public SvnKitProcessor getSvnKitProcessor() {
        return (SvnKitProcessor) getProcessor();
    }

    /**
     * Logs svn exceptions and transforms them into IOExceptions to fit in the existing framework 
     * 
     * @param ex the exception 
     * @throws IOException a re-thrown exception 
     */
    private void handleSvnException(SVNException ex) throws IOException {
        String msg = "svn diff " + ex.getMessage();
        SvnConfigurationOptions.getTaskLogger().error(msg);
        throw new IOException(msg);
    }
}
