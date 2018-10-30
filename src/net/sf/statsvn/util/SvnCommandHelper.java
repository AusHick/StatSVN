/**
 * 
 */
package net.sf.statsvn.util;

import net.sf.statsvn.output.SvnConfigurationOptions;

/**
 * @author jpdaigle
 *
 * Utility class to help build svn command strings
 */
public final class SvnCommandHelper {
	private SvnCommandHelper() {
	}

	/**
	 * Gets the authentication / non-interactive command part to use when invoking
	 * the subversion binary.
	 * 
	 * @return A String with the username, password and non-interactive settings
	 */
	public static String getAuthString() {
		final StringBuffer strAuth = new StringBuffer(" --non-interactive");
		if (SvnConfigurationOptions.getSvnUsername() != null) {
			strAuth.append(" --username ").append(SvnConfigurationOptions.getSvnUsername()).append(" --password ").append(
			        SvnConfigurationOptions.getSvnPassword());
		}

		return strAuth.toString();
	}

}
