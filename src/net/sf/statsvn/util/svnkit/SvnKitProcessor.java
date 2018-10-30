package net.sf.statsvn.util.svnkit;

import java.io.File;

import net.sf.statsvn.output.SvnConfigurationOptions;
import net.sf.statsvn.util.ISvnDiffProcessor;
import net.sf.statsvn.util.ISvnInfoProcessor;
import net.sf.statsvn.util.ISvnProcessor;
import net.sf.statsvn.util.ISvnPropgetProcessor;
import net.sf.statsvn.util.ISvnVersionProcessor;

import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * Base svnkit processor. 
 * 
 * @author jkealey, yogesh
 *
 */
public class SvnKitProcessor implements ISvnProcessor {

    
    private File checkoutDirectory;
    private ISvnDiffProcessor diffProcessorInstance;
    
    private ISvnInfoProcessor infoProcessorInstance;
    private SVNClientManager manager;
    
    private ISvnPropgetProcessor propgetProcessorInstance;
    private ISvnVersionProcessor versionProcessorInstance;
    
    public File getCheckoutDirectory()
    {
        if (checkoutDirectory==null)
        {
            checkoutDirectory = SvnConfigurationOptions.getCheckedOutDirectoryAsFile();
        }
        return checkoutDirectory;
    }
    public ISvnDiffProcessor getDiffProcessor()
    {
        if (diffProcessorInstance==null) diffProcessorInstance = new SvnKitDiff(this);
        return diffProcessorInstance;
    }
    
    public ISvnInfoProcessor getInfoProcessor()
    {
        if (infoProcessorInstance==null) infoProcessorInstance = new SvnKitInfo(this);
        return infoProcessorInstance;
    }
    public SVNClientManager getManager()
    {
        if (manager==null) 
        {
            // initialize 
            DAVRepositoryFactory.setup();
            SVNRepositoryFactoryImpl.setup();
            FSRepositoryFactory.setup();
            
            //readonly - configuration options are available only for reading
            DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
            options.setAuthStorageEnabled(false);
                    
            // Creates an instance of SVNClientManager providing an options driver & username & password 
            if (SvnConfigurationOptions.getSvnUsername()!=null && SvnConfigurationOptions.getSvnPassword()!=null)
                manager = SVNClientManager.newInstance(options, SvnConfigurationOptions.getSvnUsername(), SvnConfigurationOptions.getSvnPassword());
            else
                manager = SVNClientManager.newInstance(options);
        }
        return manager;
    }  
    
    public ISvnPropgetProcessor getPropgetProcessor()
    {
        if (propgetProcessorInstance==null) propgetProcessorInstance = new SvnKitPropget(this);
        return propgetProcessorInstance;
    }
    public ISvnVersionProcessor getVersionProcessor()
    {
        if (versionProcessorInstance==null) versionProcessorInstance = new SvnKitVersion(this);
        return versionProcessorInstance;
    }     
}
