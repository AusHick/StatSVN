package net.sf.statsvn.util;


/**
 * Base processor that uses the command line svn client. 
 * @author jkealey
 *
 */
public class SvnCommandLineProcessor implements ISvnProcessor {

    
    private ISvnDiffProcessor diffProcessorInstance;
    public ISvnDiffProcessor getDiffProcessor()
    {
        if (diffProcessorInstance==null) diffProcessorInstance = new SvnDiffUtils(this);
        return diffProcessorInstance;
    }
    
    private ISvnInfoProcessor infoProcessorInstance;
    public ISvnInfoProcessor getInfoProcessor()
    {
        if (infoProcessorInstance==null) infoProcessorInstance = new SvnInfoUtils(this);
        return infoProcessorInstance;
    }
    
    private ISvnPropgetProcessor propgetProcessorInstance;
    public ISvnPropgetProcessor getPropgetProcessor()
    {
        if (propgetProcessorInstance==null) propgetProcessorInstance = new SvnPropgetUtils(this);
        return propgetProcessorInstance;
    }  
    
    private ISvnVersionProcessor versionProcessorInstance;
    public ISvnVersionProcessor getVersionProcessor()
    {
        if (versionProcessorInstance==null) versionProcessorInstance = new SvnStartupUtils(this);
        return versionProcessorInstance;
    }     
}
