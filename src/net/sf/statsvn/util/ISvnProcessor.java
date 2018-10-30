package net.sf.statsvn.util;

/**
 * Interface for a base processor. Includes sub processors. 
 * 
 * @author jkealey
 *
 */
public interface ISvnProcessor {
    public abstract ISvnDiffProcessor getDiffProcessor();
    public abstract ISvnInfoProcessor getInfoProcessor();
    public abstract ISvnPropgetProcessor getPropgetProcessor();
    public abstract ISvnVersionProcessor getVersionProcessor();
}