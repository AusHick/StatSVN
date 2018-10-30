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

 $RCSfile: Main.java,v $
 Created on $Date: 2005/03/20 19:12:25 $
 */
package net.sf.statsvn;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.LogManager;

import net.sf.statcvs.Messages;
import net.sf.statcvs.input.LogSyntaxException;
import net.sf.statcvs.model.Repository;
import net.sf.statcvs.output.ConfigurationException;
import net.sf.statcvs.output.ConfigurationOptions;
import net.sf.statcvs.output.ReportConfig;
import net.sf.statcvs.pages.ReportSuiteMaker;
import net.sf.statsvn.input.Builder;
import net.sf.statsvn.input.RepositoryFileManager;
import net.sf.statsvn.input.SvnLogfileParser;
import net.sf.statsvn.output.SvnCommandLineParser;
import net.sf.statsvn.output.SvnConfigurationOptions;
import net.sf.statsvn.util.SvnVersionMismatchException;

/**
 * StatSvn Main Class; it starts the application and controls command-line
 * related stuff
 * 
 * @author Lukasz Pekacki
 * @author Richard Cyganiak
 * @version $Id: Main.java,v 1.47 2005/03/20 19:12:25 squig Exp $
 */
public final class Main {
    private static final int KB_IN_ONE_MB = 1024;

    private static final int NUMBER_OF_MS_IN_ONE_SEC = 1000;

    private static final LogManager LM = LogManager.getLogManager();

    /**
     * A utility class (only static methods) should be final and have a private
     * constructor.
     */
    private Main() {
    }

    /**
     * Main method of StatSVN
     * 
     * @param args
     *            command line options
     */
    public static void main(final String[] args) {
        init();
        verifyArguments(args);
        generate();
        System.exit(0);
    }

    private static void verifyArguments(final String[] args) {
        if (args.length == 0) {
            printProperUsageAndExit();
        }
        if (args.length == 1) {
            final String arg = args[0].toLowerCase(Locale.getDefault());
            if (arg.equals("-h") || arg.equals("-help")) {
                printProperUsageAndExit();
            } else if (arg.equals("-version")) {
                printVersionAndExit();
            }
        }

        try {
            new SvnCommandLineParser(args).parse();
        } catch (final ConfigurationException cex) {
            SvnConfigurationOptions.getTaskLogger().error(cex.getMessage());
            System.exit(1);
        }
    }

    public static void generate() {
        try {
            RepositoryFileManager manager = createRepoManager();
            String version = manager.getProcessor().getVersionProcessor().checkSvnVersionSufficient();
            final boolean isNewerDiffPossible = manager.getProcessor().getVersionProcessor().checkDiffPerRevPossible(version);
            // fall-back to older option.
            if (!isNewerDiffPossible) {
                SvnConfigurationOptions.setLegacyDiff(true);
            }

            manager.getProcessor().getInfoProcessor().checkRepoRootAvailable();
            generateDefaultHTMLSuite(manager);
        } catch (final ConfigurationException cex) {
            SvnConfigurationOptions.getTaskLogger().error(cex.getMessage());
            System.exit(1);
        } catch (final LogSyntaxException lex) {
            printLogErrorMessageAndExit(lex.getMessage());
        } catch (final IOException ioex) {
            printIoErrorMessageAndExit(ioex.getMessage());
        } catch (final OutOfMemoryError oome) {
            printOutOfMemMessageAndExit();
        } catch (final SvnVersionMismatchException ever) {
            printErrorMessageAndExit(ever.getMessage());
        }
    }

    public static void init() {
        Messages.setPrimaryResource("net.sf.statsvn.statcvs"); // primary is
        // statcvs.properties in net.sf.statsvn

        SvnConfigurationOptions.getTaskLogger().info(Messages.getString("PROJECT_NAME") + Messages.NL);
    }

    private static void initLogManager(final String loggingProperties) {
        InputStream stream = null;
        try {
            stream = Main.class.getResourceAsStream(loggingProperties);
            LM.readConfiguration(stream);
        } catch (final IOException e) {
            SvnConfigurationOptions.getTaskLogger().error("ERROR: Logging could not be initialized!");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (final IOException e) {
                    SvnConfigurationOptions.getTaskLogger().error("ERROR: could not close stream!");
                }
            }
        }
    }

    private static void printProperUsageAndExit() {
        final String cr = System.getProperty("line.separator");
        SvnConfigurationOptions.getTaskLogger().error(
        // max. 80 chars
                // 12345678901234567890123456789012345678901234567890123456789012345678901234567890
                "Usage: java -jar statsvn.jar [options] <logfile> <directory>" + cr + cr + "Required parameters:" + cr
                        + "  <logfile>          path to the svn logfile of the module" + cr
                        + "  <directory>        path to the directory of the checked out module" + cr + cr + "Some options:" + cr
                        + "  -version           print the version information and exit" + cr + "  -output-dir <dir>  directory where HTML suite will be saved"
                        + cr + "  -include <pattern> include only files matching pattern, e.g. **/*.c;**/*.h" + cr
                        + "  -exclude <pattern> exclude matching files, e.g. tests/**;docs/**" + cr
                        + "  -tags <regexp>     show matching tags in lines of code chart, e.g. version-.*" + cr
                        + "  -title <title>     Project title to be used in reports" + cr + "  -viewvc <url>      integrate with ViewVC installation at <url>"
                        + cr + "  -trac <url>        integrate with Trac at <url>" + cr + "  -bugzilla <url>    integrate with Bugzilla installation at <url>"
                        + cr + "  -username <svnusername> username to pass to svn" + cr + "  -password <svnpassword> password to pass to svn" + cr
                        + "  -verbose           print extra progress information" + cr + "  -xdoc                 optional switch output to xdoc" + cr
                        + "  -xml                  optional switch output to xml" + cr + "  -threads <int>        how many threads for svn diff (default: 25)"
                        + cr + "  -concurrency-threshold <millisec> switch to concurrent svn diff if 1st call>threshold (default: 4000)" + cr
                        + "  -dump                 dump the Repository content on console" + cr
                        + "  -charset <charset> specify the charset to use for html/xdoc\n"
                        + "  -tags-dir <directory> optional, specifies the director for tags (default '/tags/')" + cr + cr
                        + "Full options list: http://www.statsvn.org");
        System.exit(1);
    }

    private static void printVersionAndExit() {
        SvnConfigurationOptions.getTaskLogger().error("Version " + Messages.getString("PROJECT_VERSION"));
        System.exit(1);
    }

    private static void printOutOfMemMessageAndExit() {
        SvnConfigurationOptions.getTaskLogger().error("OutOfMemoryError.");
        SvnConfigurationOptions.getTaskLogger().error("Try running java with the -mx option (e.g. -mx128m for 128Mb).");
        System.exit(1);
    }

    private static void printLogErrorMessageAndExit(final String message) {
        SvnConfigurationOptions.getTaskLogger().error("Logfile parsing failed.");
        SvnConfigurationOptions.getTaskLogger().error(message);
        System.exit(1);
    }

    private static void printIoErrorMessageAndExit(final String message) {
        SvnConfigurationOptions.getTaskLogger().error(message);
        System.exit(1);
    }

    public static String printStackTrace(final Exception e) {
        try {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        } catch (final Exception e2) {
            if (e != null) {
                return e.getMessage();
            } else {
                return "";
            }
        }
    }

    private static void printErrorMessageAndExit(final String message) {
        SvnConfigurationOptions.getTaskLogger().error(message);
        System.exit(1);
    }

    /**
     * Generates HTML report. {@link net.sf.statsvn.output.ConfigurationOptions}
     * must be initialized before calling this method.
     * 
     * @throws LogSyntaxException
     *             if the logfile contains unexpected syntax
     * @throws IOException
     *             if some file can't be read or written
     * @throws ConfigurationException
     *             if a required ConfigurationOption was not set
     */
    public static void generateDefaultHTMLSuite() throws LogSyntaxException, IOException, ConfigurationException {
        generateDefaultHTMLSuite(createRepoManager());
    }

    private static RepositoryFileManager createRepoManager() {
        return new RepositoryFileManager(ConfigurationOptions.getCheckedOutDirectory());
    }

    /**
     * Generates HTML report. {@link net.sf.statsvn.output.ConfigurationOptions}
     * must be initialized before calling this method.
     * 
     * @param externalRepositoryFileManager
     *            RepositoryFileManager which is used to access the files in the
     *            repository.
     * 
     * @throws LogSyntaxException
     *             if the logfile contains unexpected syntax
     * @throws IOException
     *             if some file can't be read or written
     * @throws ConfigurationException
     *             if a required ConfigurationOption was not set
     */
    public static void generateDefaultHTMLSuite(final RepositoryFileManager repFileMan) throws LogSyntaxException, IOException, ConfigurationException {

        if (ConfigurationOptions.getLogFileName() == null) {
            throw new ConfigurationException("Missing logfile name");
        }
        if (ConfigurationOptions.getCheckedOutDirectory() == null) {
            throw new ConfigurationException("Missing checked out directory");
        }

        final long memoryUsedOnStart = Runtime.getRuntime().totalMemory();
        final long startTime = System.currentTimeMillis();

        initLogManager(ConfigurationOptions.getLoggingProperties());

        SvnConfigurationOptions.getTaskLogger().info(
                "Parsing SVN log '"
                        + ConfigurationOptions.getLogFileName()
                        + "'"
                        + (ConfigurationOptions.getExcludePattern() != null ? " exclude pattern '" + ConfigurationOptions.getExcludePattern() + "'"
                                : "No exclude pattern"));

        FileInputStream logFile = null;
        Builder builder = null;
        try {
            logFile = new FileInputStream(ConfigurationOptions.getLogFileName());
            builder = new Builder(repFileMan, ConfigurationOptions.getIncludePattern(), ConfigurationOptions.getExcludePattern(), ConfigurationOptions
                    .getSymbolicNamesPattern());
            new SvnLogfileParser(repFileMan, logFile, builder).parse();
        } finally {
            if (logFile != null) {
                logFile.close();
            }
        }

        if (ConfigurationOptions.getProjectName() == null) {
            ConfigurationOptions.setProjectName(builder.getProjectName());
        }
        if (ConfigurationOptions.getWebRepository() != null) {
            ConfigurationOptions.getWebRepository().setAtticFileNames(builder.getAtticFileNames());
        }

        SvnConfigurationOptions.getTaskLogger().info(
                "Generating report for " + ConfigurationOptions.getProjectName() + " into " + ConfigurationOptions.getOutputDir());
        SvnConfigurationOptions.getTaskLogger().info("Using " + ConfigurationOptions.getCssHandler());
        final Repository content = builder.createRepository();

        long memoryUsedOnEnd = Runtime.getRuntime().totalMemory();
        SvnConfigurationOptions.getTaskLogger().info("memory usage After Build: " + (((double) memoryUsedOnEnd - memoryUsedOnStart) / KB_IN_ONE_MB) + " kb");

        builder.clean();
        builder = null;

        // make JFreeChart work on systems without GUI
        System.setProperty("java.awt.headless", "true");

        final ReportConfig config = new ReportConfig(content, ConfigurationOptions.getProjectName(), ConfigurationOptions.getOutputDir(), ConfigurationOptions
                .getMarkupSyntax(), ConfigurationOptions.getCssHandler(), ConfigurationOptions.getCharSet());
        config.setWebRepository(ConfigurationOptions.getWebRepository());
        config.setWebBugtracker(ConfigurationOptions.getWebBugtracker());
        config.setNonDeveloperLogins(ConfigurationOptions.getNonDeveloperLogins());

        validate(config);

        if (SvnConfigurationOptions.isDumpContent()) {
            new RepoDump(content).dump();
        } else {
            // add new reports
            final List extraReports = new ArrayList();

            if ("xml".equalsIgnoreCase(ConfigurationOptions.getOutputFormat())) {
                new ReportSuiteMaker(config, ConfigurationOptions.getNotes(), extraReports).toXml();
            } else {
                new ReportSuiteMaker(config, ConfigurationOptions.getNotes(), extraReports).toFile().write();
            }
        }
        final long endTime = System.currentTimeMillis();
        memoryUsedOnEnd = Runtime.getRuntime().totalMemory();

        SvnConfigurationOptions.getTaskLogger().info("runtime: " + (((double) endTime - startTime) / NUMBER_OF_MS_IN_ONE_SEC) + " seconds");
        SvnConfigurationOptions.getTaskLogger().info("memory usage: " + (((double) memoryUsedOnEnd - memoryUsedOnStart) / KB_IN_ONE_MB) + " kb");
    }

    private static void validate(final ReportConfig config) {
        if (config.getRepository() == null || config.getRepository().getRoot() == null || config.getRepository().getDirectories() == null) {
            String cr = System.getProperty("line.separator");
            printErrorMessageAndExit("The repository object is not valid. Please check your settings." + cr + "Possible reasons:" + cr
                    + "1/ Did you use the option -v to create the SVN log" + cr + "2/ Is the log file empty?" + cr
                    + "3/ Do you run from a checked out directory (you should)?" + cr + "4/ Do you have non-committed items?");
        }
    }
}
