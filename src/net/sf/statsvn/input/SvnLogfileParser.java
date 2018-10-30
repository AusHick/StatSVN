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
 
 $RCSfile: SvnLogfileParser.java,v $ 
 Created on $Date: 2004/10/10 11:29:07 $ 
 */

package net.sf.statsvn.input;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.statcvs.input.LogSyntaxException;
import net.sf.statsvn.output.SvnConfigurationOptions;
import net.sf.statsvn.util.BinaryDiffException;
import net.sf.statsvn.util.FilenameComparator;
import net.sf.statsvn.util.SvnDiffUtils;
import net.sf.statsvn.util.XMLUtil;

import org.xml.sax.SAXException;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Parses a Subversion logfile and does post-parse processing. A {@link Builder}
 * must be specified which does the construction work.
 * 
 * @author Jason Kealey <jkealey@shade.ca>
 * @author Gunter Mussbacher <gunterm@site.uottawa.ca>
 * 
 * @version $Id: SvnLogfileParser.java 368 2008-06-25 21:23:46Z benoitx $
 */
public class SvnLogfileParser {
    private static final int INTERMEDIARY_SAVE_INTERVAL_MS = 120000;

    private static final String REPOSITORIES_XML = "repositories.xml";

    private final SvnLogBuilder builder;

    private final InputStream logFile;

    private final RepositoryFileManager repositoryFileManager;

    private CacheBuilder cacheBuilder;

    private HashSet revsForNewDiff = null;

    /**
     * Default Constructor
     * 
     * @param repositoryFileManager
     *            the repository file manager
     * @param logFile
     *            a <tt>Reader</tt> containing the SVN logfile
     * @param builder
     *            the builder that will process the log information
     */
    public SvnLogfileParser(final RepositoryFileManager repositoryFileManager, final InputStream logFile, final SvnLogBuilder builder) {
        this.logFile = logFile;
        this.builder = builder;
        this.repositoryFileManager = repositoryFileManager;
    }

    /**
     * Because the log file does not contain the lines added or removed in a
     * commit, and because the logfile contains implicit actions (@link
     * #verifyImplicitActions()), we must query the repository for line
     * differences. This method uses the (@link LineCountsBuilder) to load the
     * persisted information and (@link SvnDiffUtils) to find new information.
     * 
     * @param factory
     *            the factory used to create SAX parsers.
     * @throws IOException
     */
    protected void handleLineCounts(final SAXParserFactory factory) throws IOException {
        long startTime = System.currentTimeMillis();
        final String xmlFile = SvnConfigurationOptions.getCacheDir() + REPOSITORIES_XML;

        final RepositoriesBuilder repositoriesBuilder = readAndParseXmlFile(factory, xmlFile);
        cacheFileName = SvnConfigurationOptions.getCacheDir() + repositoriesBuilder.getFileName(repositoryFileManager.getRepositoryUuid());
        XMLUtil.writeXmlFile(repositoriesBuilder.getDocument(), xmlFile);
        SvnConfigurationOptions.getTaskLogger().log("parsing repositories finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        startTime = System.currentTimeMillis();

        readCache(factory);
        SvnConfigurationOptions.getTaskLogger().log("parsing line counts finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        startTime = System.currentTimeMillis();

        // update the cache xml file with the latest binary status information
        // from the working copy
        cacheBuilder.updateBinaryStatus(builder.getFileBuilders().values(), repositoryFileManager.getRootRevisionNumber());

        final Collection fileBuilders = builder.getFileBuilders().values();

        calculateNumberRequiredCalls(fileBuilders);

        // concurrency
        ExecutorService poolService = null;
        if (SvnConfigurationOptions.getNumberSvnDiffThreads() > 1) {
            poolService = Executors.newFixedThreadPool(SvnConfigurationOptions.getNumberSvnDiffThreads());
        }

        boolean isFirstDiff = true;
        calls = 0;
        groupStart = System.currentTimeMillis();
        boolean poolUseRequired = false;

        if (SvnConfigurationOptions.isLegacyDiff()) {
            for (final Iterator iter = fileBuilders.iterator(); iter.hasNext();) {
                final FileBuilder fileBuilder = (FileBuilder) iter.next();
                final String fileName = fileBuilder.getName();
                if (fileBuilder.isBinary() || !builder.matchesPatterns(fileName)) {
                    continue;
                }
                final List revisions = fileBuilder.getRevisions();
                for (int i = 0; i < revisions.size(); i++) {
                    // line diffs are expensive operations. therefore, the
                    // result is
                    // stored in the
                    // cacheBuilder and eventually persisted in the cache xml
                    // file.
                    // the next time
                    // the file is read the line diffs (or 0/0 in case of binary
                    // files) are intialized
                    // in the RevisionData. this cause hasNoLines to be false
                    // which
                    // in turn causes the
                    // if clause below to be skipped.
                    if (i + 1 < revisions.size() && ((RevisionData) revisions.get(i)).hasNoLines() && !((RevisionData) revisions.get(i)).isDeletion()) {
                        if (((RevisionData) revisions.get(i + 1)).isDeletion()) {
                            continue;
                        }
                        final String revNrNew = ((RevisionData) revisions.get(i)).getRevisionNumber();
                        if (cacheBuilder.isBinary(fileName, revNrNew)) {
                            continue;
                        }
                        final String revNrOld = ((RevisionData) revisions.get(i + 1)).getRevisionNumber();

                        if (isFirstDiff) {
                            SvnConfigurationOptions.getTaskLogger().info("Contacting server to obtain line count information.");
                            SvnConfigurationOptions.getTaskLogger().info(
                                    "This information will be cached so that the next time you run StatSVN, results will be returned more quickly.");

                            if (SvnConfigurationOptions.isLegacyDiff()) {
                                SvnConfigurationOptions.getTaskLogger().info("Using the legacy Subversion 1.3 diff mechanism: one diff per file per revision.");
                            } else {
                                SvnConfigurationOptions.getTaskLogger().info("Using the Subversion 1.4 diff mechanism: one diff per revision.");
                            }

                            isFirstDiff = false;
                        }

                        final DiffTask diff = new DiffTask(fileName, revNrNew, revNrOld, fileBuilder);

                        // SvnConfigurationOptions.getTaskLogger().log(Thread.currentThread().getName()
                        // + " Schedule task for " + fileName + " rev:" +
                        // revNrNew);

                        poolUseRequired = executeTask(poolService, poolUseRequired, diff);
                    }
                }
            }
        } else {
            for (final Iterator iter = revsForNewDiff.iterator(); iter.hasNext();) {
                final String revNrNew = (String) iter.next();
                final PerRevDiffTask diff = new PerRevDiffTask(revNrNew, builder.getFileBuilders());

                poolUseRequired = executeTask(poolService, poolUseRequired, diff);
            }

        }
        waitForPoolIfRequired(poolService);
        SvnConfigurationOptions.getTaskLogger().log("parsing svn diff");
        XMLUtil.writeXmlFile(cacheBuilder.getDocument(), cacheFileName);
        SvnConfigurationOptions.getTaskLogger().log("parsing svn diff finished in " + (System.currentTimeMillis() - startTime) + " ms.");
    }

    private boolean executeTask(final ExecutorService poolService, boolean poolUseRequired, final DiffTask diff) {
        if (poolUseRequired && SvnConfigurationOptions.getNumberSvnDiffThreads() > 1) {
            poolService.execute(diff);
        } else {
            final long start = System.currentTimeMillis();
            diff.run();
            final long end = System.currentTimeMillis();
            poolUseRequired = (end - start) > SvnConfigurationOptions.getThresholdInMsToUseConcurrency();
        }
        return poolUseRequired;
    }

    private void waitForPoolIfRequired(final ExecutorService poolService) {
        if (SvnConfigurationOptions.getNumberSvnDiffThreads() > 1 && poolService != null) {
            SvnConfigurationOptions.getTaskLogger().info(
                    "Scheduled " + requiredDiffCalls + " svn diff calls on " + Math.min(requiredDiffCalls, SvnConfigurationOptions.getNumberSvnDiffThreads())
                            + " threads.");
            poolService.shutdown();
            try {
                SvnConfigurationOptions.getTaskLogger().log("================ Wait for completion =========================");
                if (!poolService.awaitTermination(2, TimeUnit.DAYS)) {
                    SvnConfigurationOptions.getTaskLogger().log("================ TIME OUT!!! =========================");
                }
            } catch (final InterruptedException e) {
                SvnConfigurationOptions.getTaskLogger().error(e.toString());
            }
        }
    }

    private void calculateNumberRequiredCalls(final Collection fileBuilders) {
        // Calculate the number of required calls...
        requiredDiffCalls = 0;

        if (!SvnConfigurationOptions.isLegacyDiff()) {
            revsForNewDiff = new HashSet();
        }

        for (final Iterator iter = fileBuilders.iterator(); iter.hasNext();) {
            final FileBuilder fileBuilder = (FileBuilder) iter.next();
            final String fileName = fileBuilder.getName();
            if (!fileBuilder.isBinary() && builder.matchesPatterns(fileName)) {
                final List revisions = fileBuilder.getRevisions();
                for (int i = 0; i < revisions.size(); i++) {
                    if (i + 1 < revisions.size() && ((RevisionData) revisions.get(i)).hasNoLines() && !((RevisionData) revisions.get(i)).isDeletion()) {
                        if (((RevisionData) revisions.get(i + 1)).isDeletion()) {
                            continue;
                        }
                        final String revNrNew = ((RevisionData) revisions.get(i)).getRevisionNumber();
                        if (cacheBuilder.isBinary(fileName, revNrNew)) {
                            continue;
                        }
                        // count if legacy diff or this rev wasn't already
                        // counted.
                        if (revsForNewDiff == null || !revsForNewDiff.contains(revNrNew)) {
                            requiredDiffCalls++;

                            if (revsForNewDiff != null) {
                                revsForNewDiff.add(revNrNew);
                            }
                        }
                    }
                }
            }
        }
        // END Calculate the number of required calls...
    }

    private void readCache(final SAXParserFactory factory) throws IOException {
        cacheBuilder = new CacheBuilder(builder, repositoryFileManager);
        FileInputStream cacheFile = null;
        try {
            cacheFile = new FileInputStream(cacheFileName);
            final SAXParser parser = factory.newSAXParser();
            parser.parse(cacheFile, new SvnXmlCacheFileHandler(cacheBuilder));
            cacheFile.close();
        } catch (final ParserConfigurationException e) {
            SvnConfigurationOptions.getTaskLogger().error("Cache: " + e.toString());
        } catch (final SAXException e) {
            SvnConfigurationOptions.getTaskLogger().error("Cache: " + e.toString());
        } catch (final FileNotFoundException e) {
            SvnConfigurationOptions.getTaskLogger().log("Cache: " + e.toString());
        } catch (final IOException e) {
            SvnConfigurationOptions.getTaskLogger().error("Cache: " + e.toString());
        } finally {
            if (cacheFile != null) {
                cacheFile.close();
            }
        }
    }

    private RepositoriesBuilder readAndParseXmlFile(final SAXParserFactory factory, final String xmlFile) throws IOException {
        final RepositoriesBuilder repositoriesBuilder = new RepositoriesBuilder();
        FileInputStream repositoriesFile = null;
        try {
            repositoriesFile = new FileInputStream(xmlFile);
            final SAXParser parser = factory.newSAXParser();
            parser.parse(repositoriesFile, new SvnXmlRepositoriesFileHandler(repositoriesBuilder));
            repositoriesFile.close();
        } catch (final ParserConfigurationException e) {
            SvnConfigurationOptions.getTaskLogger().error("Repositories: " + e.toString());
        } catch (final SAXException e) {
            SvnConfigurationOptions.getTaskLogger().error("Repositories: " + e.toString());
        } catch (final FileNotFoundException e) {
            SvnConfigurationOptions.getTaskLogger().log("Repositories: " + e.toString());
        } catch (final IOException e) {
            SvnConfigurationOptions.getTaskLogger().error("Repositories: " + e.toString());
        } finally {
            if (repositoriesFile != null) {
                repositoriesFile.close();
            }
        }
        return repositoriesBuilder;
    }

    /**
     * Parses the logfile. After <tt>parse()</tt> has finished, the result of
     * the parsing process can be obtained from the builder.
     * 
     * @throws LogSyntaxException
     *             if syntax errors in log
     * @throws IOException
     *             if errors while reading from the log Reader
     */
    public void parse() throws LogSyntaxException, IOException {

        final SAXParserFactory factory = parseSvnLog();

        verifyImplicitActions();

        // must be after verifyImplicitActions();
        removeDirectories();

        handleLineCounts(factory);

    }

    /**
     * The svn log can contain deletions of directories which imply that all of
     * its contents have been deleted.
     * 
     * Furthermore, the svn log can contain entries which are copies from other
     * directories (additions or replacements; I haven't seen modifications with
     * this property, but am not 100% sure) meaning that all files from the
     * other directory are copied here. We currently do not go back through
     * copies, so we must infer what files <i>could</i> have been added during
     * those copies.
     * 
     */
    protected void verifyImplicitActions() {
        // this method most certainly has issues with implicit actions on root
        // folder.

        final long startTime = System.currentTimeMillis();
        SvnConfigurationOptions.getTaskLogger().log("verifying implicit actions ...");

        final HashSet implicitActions = new HashSet();

        // get all filenames
        final ArrayList files = new ArrayList();
        final Collection fileBuilders = fetchAllFileNames(files);

        // sort them so that folders are immediately followed by the folder
        // entries and then by other files which are prefixed by the folder
        // name.
        Collections.sort(files, new FilenameComparator());

        // for each file
        for (int i = 0; i < files.size(); i++) {
            final String parent = files.get(i).toString();
            final FileBuilder parentBuilder = (FileBuilder) builder.getFileBuilders().get(parent);
            // check to see if there are files that indicate that parent is a
            // folder.
            for (int j = i + 1; j < files.size() && files.get(j).toString().indexOf(parent + "/") == 0; j++) {
                // we might not know that it was a folder.
                repositoryFileManager.addDirectory(parent);

                final String child = files.get(j).toString();
                final FileBuilder childBuilder = (FileBuilder) builder.getFileBuilders().get(child);
                // for all revisions in the the parent folder
                for (final Iterator iter = parentBuilder.getRevisions().iterator(); iter.hasNext();) {
                    final RevisionData parentData = (RevisionData) iter.next();
                    int parentRevision;
                    try {
                        parentRevision = Integer.parseInt(parentData.getRevisionNumber());
                    } catch (final Exception e) {
                        continue;
                    }

                    // ignore modifications to folders
                    if (parentData.isCreationOrRestore() || parentData.isDeletion()) {
                        int k;

                        // check to see if the parent revision is an implicit
                        // action acting on the child.
                        k = detectActionOnChildGivenActionOnParent(childBuilder, parentRevision);

                        // we found something to insert
                        if (k < childBuilder.getRevisions().size()) {
                            createImplicitAction(implicitActions, child, childBuilder, parentData, k);
                        }
                    }
                }
            }
        }

        // Some implicit revisions may have resulted in double deletion
        // (e.g. deleting a directory and THEN deleting the parent directory).
        // this will get rid of any consecutive deletion.
        cleanPotentialDuplicateImplicitActions(fileBuilders);

        // in the preceeding block, we add implicit additions to too may files.
        // possibly a folder was deleted and restored later on, without the
        // specific file being re-added. we get rid of those here. however,
        // without knowledge of what was copied during the implicit additions /
        // replacements, we will remove as many implicit actions as possible
        // 
        // this solution is imperfect.

        // Examples:
        // IA ID IA ID M A -> ID M A
        // IA ID A D M A -> ID A D M A
        removePotentialInconsistencies(implicitActions, fileBuilders);
        SvnConfigurationOptions.getTaskLogger().log("verifying implicit actions finished in " + (System.currentTimeMillis() - startTime) + " ms.");
    }

    private void createImplicitAction(final HashSet implicitActions, final String child, final FileBuilder childBuilder, final RevisionData parentData,
            final int k) {
        // we want to memorize this implicit action.
        final RevisionData implicit = parentData.createCopy();
        implicitActions.add(implicit);

        // avoid concurrent modification errors.
        final List toMove = new ArrayList();
        for (final Iterator it = childBuilder.getRevisions().subList(k, childBuilder.getRevisions().size()).iterator(); it.hasNext();) {
            final RevisionData revToMove = (RevisionData) it.next();
            // if
            // (!revToMove.getRevisionNumber().equals(implicit.getRevisionNumber()))
            // {
            toMove.add(revToMove);
            // }
        }

        // remove the revisions to be moved.
        childBuilder.getRevisions().removeAll(toMove);

        // don't call addRevision directly. buildRevision
        // does more.
        builder.buildFile(child, false, false, new HashMap(), new HashMap());

        // only add the implicit if the last one for the
        // file is NOT a deletion!
        // if (!toMove.isEmpty() && !((RevisionData)
        // toMove.get(0)).isDeletion()) {
        builder.buildRevision(implicit);
        // }

        // copy back the revisions we removed.
        for (final Iterator it = toMove.iterator(); it.hasNext();) {
            builder.buildRevision((RevisionData) it.next());
        }
    }

    private int detectActionOnChildGivenActionOnParent(final FileBuilder childBuilder, final int parentRevision) {
        int k;
        for (k = 0; k < childBuilder.getRevisions().size(); k++) {
            final RevisionData childData = (RevisionData) childBuilder.getRevisions().get(k);
            final int childRevision = Integer.parseInt(childData.getRevisionNumber());

            // we don't want to add duplicate entries for the
            // same revision
            if (parentRevision == childRevision) {
                k = childBuilder.getRevisions().size();
                break;
            }

            if (parentRevision > childRevision) {
                break; // we must insert it here!
            }
        }
        return k;
    }

    private void removePotentialInconsistencies(final HashSet implicitActions, final Collection fileBuilders) {
        for (final Iterator iter = fileBuilders.iterator(); iter.hasNext();) {
            final FileBuilder filebuilder = (FileBuilder) iter.next();

            // make sure our attic is well set, with our new deletions that we
            // might have added.
            if (!repositoryFileManager.existsInWorkingCopy(filebuilder.getName())) {
                builder.addToAttic(filebuilder.getName());
            }

            // do we detect an inconsistency?
            if (!repositoryFileManager.existsInWorkingCopy(filebuilder.getName()) && !filebuilder.finalRevisionIsDead()) {
                int earliestDelete = -1;
                for (int i = 0; i < filebuilder.getRevisions().size(); i++) {
                    final RevisionData data = (RevisionData) filebuilder.getRevisions().get(i);

                    if (data.isDeletion()) {
                        earliestDelete = i;
                    }

                    if ((!data.isCreationOrRestore() && data.isChange()) || !implicitActions.contains(data)) {
                        break;
                    }
                }

                if (earliestDelete > 0) {
                    // avoid concurrent modification errors.
                    final List toRemove = new ArrayList();
                    for (final Iterator it = filebuilder.getRevisions().subList(0, earliestDelete).iterator(); it.hasNext();) {
                        toRemove.add(it.next());
                    }
                    filebuilder.getRevisions().removeAll(toRemove);
                }
            }
        }
    }

    private void cleanPotentialDuplicateImplicitActions(final Collection fileBuilders) {
        for (final Iterator iter = fileBuilders.iterator(); iter.hasNext();) {
            final FileBuilder filebuilder = (FileBuilder) iter.next();

            boolean previousIsDelete = false;
            final List toRemove = new ArrayList();
            // for this file, iterate through all revisions and store any
            // deletion revision that follows
            // a deletion.
            for (final Iterator it = filebuilder.getRevisions().iterator(); it.hasNext();) {
                final RevisionData data = (RevisionData) it.next();
                if (data.isDeletion() && previousIsDelete) {
                    toRemove.add(data);
                }
                previousIsDelete = data.isDeletion();
            }

            // get rid of the duplicate deletion for this file.
            if (!toRemove.isEmpty()) {
                filebuilder.getRevisions().removeAll(toRemove);
            }
        }
    }

    private Collection fetchAllFileNames(final ArrayList files) {
        final Collection fileBuilders = builder.getFileBuilders().values();
        for (final Iterator iter = fileBuilders.iterator(); iter.hasNext();) {
            final FileBuilder fileBuilder = (FileBuilder) iter.next();
            files.add(fileBuilder.getName());
        }
        return fileBuilders;
    }

    /**
     * We have created FileBuilders for directories because we needed the
     * information to be able to find implicit actions. However, we don't want
     * to query directories for their line counts later on. Therefore, we must
     * remove them here.
     * 
     * (@link SvnInfoUtils#isDirectory(String)) is used to know what files are
     * directories. Deleted directories are assumed to have been added in (@link
     * #verifyImplicitActions())
     */
    protected void removeDirectories() {
        final Collection fileBuilders = builder.getFileBuilders().values();
        final ArrayList toRemove = new ArrayList();
        for (final Iterator iter = fileBuilders.iterator(); iter.hasNext();) {
            final FileBuilder fileBuilder = (FileBuilder) iter.next();
            if (repositoryFileManager.isDirectory(fileBuilder.getName())) {
                toRemove.add(fileBuilder.getName());
            }
        }

        for (final Iterator iter = toRemove.iterator(); iter.hasNext();) {
            builder.getFileBuilders().remove(iter.next());
        }

    }

    /**
     * Parses the svn log file.
     * 
     * @return the SaxParserFactory, so that it can be reused.
     * @throws IOException
     *             errors while reading file.
     * @throws LogSyntaxException
     *             invalid log syntax.
     */
    protected SAXParserFactory parseSvnLog() throws IOException, LogSyntaxException {
        final long startTime = System.currentTimeMillis();
        SvnConfigurationOptions.getTaskLogger().log("starting to parse...");

        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            final SAXParser parser = factory.newSAXParser();
            parser.parse(logFile, new SvnXmlLogFileHandler(builder, repositoryFileManager));
        } catch (final ParserConfigurationException e) {
            throw new LogSyntaxException("svn log: " + e.getMessage());
        } catch (final SAXException e) {
            throw new LogSyntaxException("svn log: " + e.getMessage());
        }

        SvnConfigurationOptions.getTaskLogger().log("parsing svn log finished in " + (System.currentTimeMillis() - startTime) + " ms.");
        return factory;
    }

    private long totalTime = 0;

    private long groupStart = 0;

    private int calls = 0;

    private int requiredDiffCalls = 0;

    private String cacheFileName;

    protected class DiffTask implements Runnable {
        private String fileName;
        private String newRevision;
        private String oldRevision;
        private FileBuilder fileBuilder;

        protected DiffTask() {
        }

        protected DiffTask(final String newRevision) {
            super();
            this.newRevision = newRevision;
        }

        public DiffTask(final String fileName, final String newRevision, final String oldRevision, final FileBuilder fileBuilder) {
            super();
            this.fileName = fileName;
            this.newRevision = newRevision;
            this.oldRevision = oldRevision;
            this.fileBuilder = fileBuilder;
        }

        /**
         * @return the fileName
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * @param fileName
         *            the fileName to set
         */
        public void setFileName(final String fileName) {
            this.fileName = fileName;
        }

        /**
         * @return the newRevision
         */
        public String getNewRevision() {
            return newRevision;
        }

        /**
         * @param newRevision
         *            the newRevision to set
         */
        public void setNewRevision(final String newRevision) {
            this.newRevision = newRevision;
        }

        /**
         * @return the oldRevision
         */
        public String getOldRevision() {
            return oldRevision;
        }

        /**
         * @param oldRevision
         *            the oldRevision to set
         */
        public void setOldRevision(final String oldRevision) {
            this.oldRevision = oldRevision;
        }

        public void run() {
            int[] lineDiff;
            long end = 0L;
            try {
                // SvnConfigurationOptions.getTaskLogger().log(Thread.currentThread().getName()
                // + " Starts... now");
                final long start = System.currentTimeMillis();
                lineDiff = repositoryFileManager.getLineDiff(oldRevision, newRevision, fileName);
                end = System.currentTimeMillis();
                synchronized (cacheBuilder) {
                    totalTime += (end - start);
                }

                SvnConfigurationOptions.getTaskLogger().info(
                        "svn diff " + (++calls) + "/" + requiredDiffCalls + ": " + fileName + ", r" + oldRevision + " to r" + newRevision + ", +" + lineDiff[0]
                                + " -" + lineDiff[1] + " (" + (end - start) + " ms.) " + Thread.currentThread().getName());
            } catch (final BinaryDiffException e) {
                calls++;
                trackBinaryFile();
                return;
            } catch (final IOException e) {
                SvnConfigurationOptions.getTaskLogger()
                        .error("" + (++calls) + "/" + requiredDiffCalls + " IOException: Unable to obtain diff: " + e.toString());
                return;
            }

            trackFileDiff(lineDiff);

            performIntermediarySave(end);
        }

        protected void trackBinaryFile() {
            // file is binary and has been deleted
            cacheBuilder.newRevision(fileName, newRevision, "0", "0", true);
            fileBuilder.setBinary(true);
        }

        protected void trackFileDiff(final int[] lineDiff) {
            if (lineDiff[0] != -1 && lineDiff[1] != -1) {
                builder.updateRevision(fileName, newRevision, lineDiff[0], lineDiff[1]);
                cacheBuilder.newRevision(fileName, newRevision, lineDiff[0] + "", lineDiff[1] + "", false);
            } else {
                SvnConfigurationOptions.getTaskLogger().info("unknown behaviour; to be investigated:" + fileName + " r:" + oldRevision + "/r:" + newRevision);
            }
        }

        protected void performIntermediarySave(long end) {
            synchronized (cacheBuilder) {
                if (end - groupStart > INTERMEDIARY_SAVE_INTERVAL_MS) {
                    final long start = System.currentTimeMillis();
                    XMLUtil.writeXmlFile(cacheBuilder.getDocument(), cacheFileName);
                    groupStart = System.currentTimeMillis();
                    final double estimateLeftInMs = ((double) totalTime / (double) calls * (requiredDiffCalls - calls) / SvnConfigurationOptions
                            .getNumberSvnDiffThreads());
                    end = System.currentTimeMillis();
                    SvnConfigurationOptions.getTaskLogger().info(
                            System.getProperty("line.separator") + new Date() + " Intermediary save took " + (end - start) + " ms. Estimated completion="
                                    + new Date(end + (long) estimateLeftInMs) + System.getProperty("line.separator"));
                }
            }
        }

        protected FileBuilder getFileBuilder() {
            return fileBuilder;
        }

        protected void setFileBuilder(final FileBuilder fileBuilder) {
            this.fileBuilder = fileBuilder;
        }

    }

    protected class PerRevDiffTask extends DiffTask {
        private Map fileBuilders;

        public PerRevDiffTask(final String newRevision, final Map fileBuilders) {
            super(newRevision);
            this.fileBuilders = fileBuilders;
        }

        public void run() {
            int[] lineDiff;
            Vector results;
            long end = 0L;
            try {
                // SvnConfigurationOptions.getTaskLogger().log(Thread.currentThread().getName()
                // + " Starts... now");
                final long start = System.currentTimeMillis();
                results = repositoryFileManager.getRevisionDiff(getNewRevision());
                end = System.currentTimeMillis();
                synchronized (cacheBuilder) {
                    totalTime += (end - start);
                }

                SvnConfigurationOptions.getTaskLogger().info(
                        "svn diff " + (++calls) + "/" + requiredDiffCalls + " on r" + getNewRevision() + " (" + (end - start) + " ms.) "
                                + Thread.currentThread().getName());

                for (int i = 0; i < results.size(); i++) {
                    final Object[] element = (Object[]) results.get(i);

                    if (element.length == SvnDiffUtils.RESULT_SIZE && fileBuilders.containsKey(element[0].toString())) {
                        setFileName(element[0].toString());
                        setFileBuilder((FileBuilder) fileBuilders.get(getFileName()));
                        lineDiff = (int[]) element[1];
                        setOldRevision("?");

                        final Boolean isBinary = (Boolean) element[2];
                        if (isBinary.booleanValue()) {
                            trackBinaryFile();
                        }

                        SvnConfigurationOptions.getTaskLogger().info(
                                "\t " + getFileName() + ", on r" + getNewRevision() + ", +" + lineDiff[0] + " -" + lineDiff[1]);

                        trackFileDiff(lineDiff);
                    } else {
                        SvnConfigurationOptions.getTaskLogger().error("Problem with diff " + i + " for revision " + getNewRevision() + ".");
                    }
                }

            } catch (final BinaryDiffException e) {
                // not supposed to happen. tracked individually.
                return;
            } catch (final IOException e) {
                SvnConfigurationOptions.getTaskLogger()
                        .error("" + (++calls) + "/" + requiredDiffCalls + " IOException: Unable to obtain diff: " + e.toString());
                return;
            }

            performIntermediarySave(end);
        }
    }
}
