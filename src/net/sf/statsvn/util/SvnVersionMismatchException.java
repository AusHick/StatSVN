package net.sf.statsvn.util;

/**
 * Indicates that an invalid version of the <tt>svn</tt> executable was found.
 * This exception can be thrown by explicit checking of the <tt>svn</tt> binary's version, or
 * by checking for (and failing to find) a repository root in <tt>svn info</tt>'s output (a 1.3 feature).
 * 
 * @see net.sf.statsvn.util.SvnStartupUtils
 * 
 * @author Jean-Philippe Daigle <jpdaigle@softwareengineering.ca>
 * 
 * @version $Id: SvnVersionMismatchException.java 351 2008-03-28 18:46:26Z benoitx $
 */
public class SvnVersionMismatchException extends Exception {
	private static final long serialVersionUID = 1L;

	public SvnVersionMismatchException() {
		super("Subversion binary is incorrect version or not found. Please verify that "
		        + "you have installed the Subversion command-line client and it is on your path.");
	}

	public SvnVersionMismatchException(final String m) {
		super(m);
	}

	public SvnVersionMismatchException(final String found, final String required) {
		super("Subversion binary is incorrect version. Found: " + found + ", required: " + required);
	}
}
