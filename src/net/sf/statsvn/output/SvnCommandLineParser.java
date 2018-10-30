/*
 StatCvs - CVS statistics generation 
 Copyright (C) 2002  Lukasz Pekacki <lukasz@pekacki.de>
 http://statcvs.sf.net/
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
 $RCSfile: SvnCommandLineParser.java,v $
 Created on $Date: 2005/03/20 19:12:25 $ 
 */
package net.sf.statsvn.output;

import java.util.Locale;

import net.sf.statcvs.output.CommandLineParser;
import net.sf.statcvs.output.ConfigurationException;
import net.sf.statcvs.output.ConfigurationOptions;
import net.sf.statcvs.output.CvswebIntegration;
import net.sf.statcvs.output.ViewCvsIntegration;
import net.sf.statcvs.output.ViewVcIntegration;
import net.sf.statcvs.output.WebRepositoryIntegration;

/**
 * Takes a command line, like given to the {@link net.sf.statsvn.Main#main}
 * method, and turns it into a {@link ConfigurationOptions} object.
 * 
 * @author Richard Cyganiak <rcyg@gmx.de>
 * @version $Id: SvnCommandLineParser.java,v 1.16 2005/03/20 19:12:25 squig Exp $
 */
public class SvnCommandLineParser extends CommandLineParser {
	private boolean setCacheDir = false;

	/**
	 * Constructor for SvnCommandLineParser
	 * 
	 * @param args
	 *            the command line parameters
	 */
	public SvnCommandLineParser(final String[] args) {
		super(args);
	}

	protected boolean doChildrenSwitch(final String switchName) throws ConfigurationException {
		final String s = switchName.toLowerCase(Locale.getDefault());
		if (s.equals("cache-dir")) {
			if (isArgsEmpty()) {
				throw new ConfigurationException("Missing argument for -cache-dir");
			}
			SvnConfigurationOptions.setCacheDir(popNextArg());
			setCacheDir = true;
		} else if (s.equals("username")) {
			if (isArgsEmpty()) {
				throw new ConfigurationException("Missing argument for -username");
			}
			SvnConfigurationOptions.setSvnUsername(popNextArg());
		} else if (s.equals("password")) {
			if (isArgsEmpty()) {
				throw new ConfigurationException("Missing argument for -password");
			}
			SvnConfigurationOptions.setSvnPassword(popNextArg());
		} else if (s.equals("threads")) {
			if (isArgsEmpty()) {
				throw new ConfigurationException("Missing argument for -threads");
			}
			SvnConfigurationOptions.setNumberSvnDiffThreads(Integer.parseInt(popNextArg()));
		} else if (s.equals("concurrency-threshold")) {
			if (isArgsEmpty()) {
				throw new ConfigurationException("Missing argument for -concurrency-threshold");
			}
			SvnConfigurationOptions.setThresholdInMsToUseConcurrency(Integer.parseInt(popNextArg()));
		} else if (s.equals("tags-dir")) {
			if (isArgsEmpty()) {
				throw new ConfigurationException("Missing argument for -tags-dir");
			}
			SvnConfigurationOptions.setTagsDirectory(popNextArg());
		} else if (s.equals("dump")) {
			SvnConfigurationOptions.setDumpContent(true);
		} else if (s.equals("anonymize")) {
			SvnConfigurationOptions.setAnonymize(true);
        } else if (s.equals("svnkit")) {
            SvnConfigurationOptions.setUsingSvnKit(true);
		} else if (s.equals("force-legacy-diff")) {
			SvnConfigurationOptions.setLegacyDiff(true);
		} else {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.statcvs.output.CommandLineParser#checkForRequiredArgs()
	 */
	protected void checkForRequiredArgs() throws ConfigurationException {
		super.checkForRequiredArgs();
		if (!setCacheDir) {
			SvnConfigurationOptions.setCacheDirToDefault();
		}
		// now check if the user may have setup some WebIntegration that are not supported
		final WebRepositoryIntegration integration = ConfigurationOptions.getWebRepository();
		if (integration instanceof ViewCvsIntegration && !(integration instanceof ViewVcIntegration)) {
			throw new ConfigurationException("Sorry, ViewCvs is not supported by Subversion");
		} else if (integration instanceof CvswebIntegration) {
			throw new ConfigurationException("Sorry, CvsWeb is not supported by Subversion");
		}
	}
}