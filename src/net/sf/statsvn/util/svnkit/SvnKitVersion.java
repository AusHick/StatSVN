package net.sf.statsvn.util.svnkit;

import net.sf.statsvn.util.ISvnProcessor;
import net.sf.statsvn.util.SvnStartupUtils;
import net.sf.statsvn.util.SvnVersionMismatchException;

/**
 * Runs svn -version using svnkit. (Possible?)
 *  
 * @author jkealey, yogesh
 */
public class SvnKitVersion extends SvnStartupUtils {

    public SvnKitVersion(ISvnProcessor processor) {
        super(processor);
    }

    public SvnKitProcessor getSvnKitProcessor() {
        return (SvnKitProcessor) getProcessor();
    }
    
    public String checkSvnVersionSufficient() throws SvnVersionMismatchException {
        // TODO: Not sure how to implement with svnkit. 
        return "1.4.0";
    }

}
