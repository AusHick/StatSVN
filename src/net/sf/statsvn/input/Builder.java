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
 
 $RCSfile: Builder.java,v $
 $Date: 2004/12/14 13:38:13 $
 */
package net.sf.statsvn.input;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import net.sf.statcvs.Messages;
import net.sf.statcvs.input.CommitListBuilder;
import net.sf.statcvs.input.NoLineCountException;
import net.sf.statcvs.model.Author;
import net.sf.statcvs.model.Directory;
import net.sf.statcvs.model.Repository;
import net.sf.statcvs.model.SymbolicName;
import net.sf.statcvs.model.VersionedFile;
import net.sf.statcvs.output.ConfigurationOptions;
import net.sf.statcvs.util.FilePatternMatcher;
import net.sf.statcvs.util.FileUtils;
import net.sf.statcvs.util.StringUtils;
import net.sf.statsvn.output.SvnConfigurationOptions;

/**
 * <p>
 * Helps building the {@link net.sf.statsvn.model.Repository} from a SVN log. The <tt>Builder</tt> is fed by some SVN history data source, for example a SVN
 * log parser. The <tt>Repository</tt> can be retrieved using the {@link #createRepository} method.
 * </p>
 * 
 * <p>
 * The class also takes care of the creation of <tt>Author</tt> and </tt>Directory</tt> objects and makes sure that there's only one of these for each
 * author name and path. It also provides LOC count services.
 * </p>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @author Jason Kealey <jkealey@shade.ca>
 * @author Gunter Mussbacher <gunterm@site.uottawa.ca>
 * 
 * @version $Id: Builder.java 389 2009-05-27 18:17:59Z benoitx $
 * 
 */
public class Builder implements SvnLogBuilder {
    private final Set atticFileNames = new HashSet();

    private final Map authors = new HashMap();

    private FileBuilder currentFileBuilder = null;

    private final Map directories = new HashMap();

    private final FilePatternMatcher excludePattern;

    private final Map fileBuilders = new HashMap();

    private final FilePatternMatcher includePattern;

    private String projectName = null;

    private final RepositoryFileManager repositoryFileManager;

    private Date startDate = null;

    private final Map symbolicNames = new HashMap();

    private final Pattern tagsPattern;
    
    public void clean() {
        atticFileNames.clear();
        authors.clear();
        directories.clear();
        fileBuilders.clear();
        symbolicNames.clear();
    }

    /**
     * Creates a new <tt>Builder</tt>
     * 
     * @param repositoryFileManager
     *            the {@link RepositoryFileManager} that can be used to retrieve LOC counts for the files that this builder will create
     * @param includePattern
     *            a list of Ant-style wildcard patterns, seperated by : or ;
     * @param excludePattern
     *            a list of Ant-style wildcard patterns, seperated by : or ;
     */
    public Builder(final RepositoryFileManager repositoryFileManager, final FilePatternMatcher includePattern, final FilePatternMatcher excludePattern,
            final Pattern tagsPattern) {
        this.repositoryFileManager = repositoryFileManager;
        this.includePattern = includePattern;
        this.excludePattern = excludePattern;
        this.tagsPattern = tagsPattern;
        directories.put("", Directory.createRoot());
    }

    /**
     * Adds a file to the attic. This method should only be called if our first invocation to (@link #buildFile(String, boolean, boolean, Map)) was given an
     * invalid isInAttic field.
     * 
     * This is a hack to handle post-processing of implicit deletions at the same time as the implicit additions that can be found in Subversion.
     * 
     * @param filename
     *            the filename to add to the attic.
     */
    public void addToAttic(final String filename) {
        if (!atticFileNames.contains(filename)) {
            atticFileNames.add(filename);
        }
    }

    /**
     * <p>
     * Starts building a new file. The files are not expected to be created in any particular order. Subsequent calls to (@link #buildRevision(RevisionData))
     * will add revisions to this file.
     * </p>
     * 
     * <p>
     * New in StatSVN: If the method has already been invoked with the same filename, the original file will be re-loaded and the other arguments are ignored.
     * </p>
     * 
     * @param filename
     *            the file's name with path, for example "path/file.txt"
     * @param isBinary
     *            <tt>true</tt> if it's a binary file
     * @param isInAttic
     *            <tt>true</tt> if the file is dead on the main branch
     * @param revBySymnames
     *            maps revision (string) by symbolic name (string)
     * @param dateBySymnames
     *            maps date (date) by symbolic name (string)
     */
    public void buildFile(final String filename, final boolean isBinary, final boolean isInAttic, final Map revBySymnames, final Map dateBySymnames) {
        if (fileBuilders.containsKey(filename)) {
            currentFileBuilder = (FileBuilder) fileBuilders.get(filename);
        } else {
            currentFileBuilder = new FileBuilder(this, filename, isBinary, revBySymnames, dateBySymnames);
            fileBuilders.put(filename, currentFileBuilder);
            if (isInAttic) {
                addToAttic(filename);
            }
        }
    }

    /**
     * Starts building the module.
     * 
     * @param moduleName
     *            name of the module
     */
    public void buildModule(final String moduleName) {
        this.projectName = moduleName;
    }

    /**
     * Adds a revision to the current file. The revisions must be added in SVN logfile order, that is starting with the most recent one.
     * 
     * @param data
     *            the revision
     */
    public void buildRevision(final RevisionData data) {

        currentFileBuilder.addRevisionData(data);

        if (startDate == null || startDate.compareTo(data.getDate()) > 0) {
            startDate = data.getDate();
        }
    }

    /**
     * Returns a Repository object of all files.
     * 
     * @return Repository a Repository object
     */
    public Repository createRepository() {

        if (startDate == null) {
            return new Repository();
        }

        final Repository result = new Repository();
        final Iterator it = fileBuilders.values().iterator();
        while (it.hasNext()) {
            final FileBuilder fileBuilder = (FileBuilder) it.next();
            final VersionedFile file = fileBuilder.createFile(startDate);
            if (file == null) {
                continue;
            }
            result.addFile(file);
            SvnConfigurationOptions.getTaskLogger().log("adding " + file.getFilenameWithPath() + " (" + file.getRevisions().size() + " revisions)");
        }

        // Uh oh...
        final SortedSet revisions = result.getRevisions();
        final List commits = new CommitListBuilder(revisions).createCommitList();
        result.setCommits(commits);

        //        result.setSymbolicNames(new TreeSet(symbolicNames.values()));
        result.setSymbolicNames(getMatchingSymbolicNames());

        SvnConfigurationOptions.getTaskLogger().log("SYMBOLIC NAMES - " + symbolicNames);

        return result;
    }

    /**
     * Returns the <tt>Set</tt> of filenames that are "in the attic".
     * 
     * @return a <tt>Set</tt> of <tt>String</tt>s
     */
    public Set getAtticFileNames() {
        return atticFileNames;
    }

    /**
     * returns the <tt>Author</tt> of the given name or creates it if it does not yet exist. Author names are handled as case-insensitive.
     * 
     * @param name
     *            the author's name
     * @return a corresponding <tt>Author</tt> object
     */
    public Author getAuthor(String name) {
        if (name == null || name.length() == 0) {
            name = Messages.getString("AUTHOR_UNKNOWN");
        }

        String lowerCaseName = name.toLowerCase(Locale.getDefault());
        final boolean bAnon = SvnConfigurationOptions.isAnonymize();
        if (this.authors.containsKey(lowerCaseName)) {
            return (Author) this.authors.get(lowerCaseName);
        }

        Author newAuthor;
        if (bAnon) {
            // The first time a particular name is encountered, create an anonymized name.
            newAuthor = new Author(AuthorAnonymizingProvider.getNewName());
        } else {
            newAuthor = new Author(name);
        }

        final Properties p = ConfigurationOptions.getConfigProperties();

        if (p != null) {
            String replacementUser = p.getProperty("user." + lowerCaseName + ".replacedBy");

            if (StringUtils.isNotEmpty(replacementUser)) {
                replacementUser = replacementUser.toLowerCase();
                if (this.authors.containsKey(replacementUser)) {
                    return (Author) this.authors.get(replacementUser);
                }
                lowerCaseName = replacementUser;
                newAuthor = new Author(lowerCaseName);
            }
        }

        if (p != null && !bAnon) {
            newAuthor.setRealName(p.getProperty("user." + lowerCaseName + ".realName"));
            newAuthor.setHomePageUrl(p.getProperty("user." + lowerCaseName + ".url"));
            newAuthor.setImageUrl(p.getProperty("user." + lowerCaseName + ".image"));
            newAuthor.setEmail(p.getProperty("user." + lowerCaseName + ".email"));
            newAuthor.setTwitterUserName(p.getProperty("user." + name.toLowerCase() + ".twitterUsername"));
            newAuthor.setTwitterUserId(p.getProperty("user." + name.toLowerCase() + ".twitterUserId"));
            String val = p.getProperty("user." + name.toLowerCase() + ".twitterIncludeFlash");
            if (val != null && val.length() > 0) {
                newAuthor.setTwitterIncludeFlash(Boolean.valueOf(val).booleanValue());
            }
            val = p.getProperty("user." + name.toLowerCase() + ".twitterIncludeHtml");
            if (val != null && val.length() > 0) {
                newAuthor.setTwitterIncludeHtml(Boolean.valueOf(val).booleanValue());
            }
        }
        this.authors.put(lowerCaseName, newAuthor);
        return newAuthor;
    }

    /**
     * Returns the <tt>Directory</tt> of the given filename or creates it if it does not yet exist.
     * 
     * @param filename
     *            the name and path of a file, for example "src/Main.java"
     * @return a corresponding <tt>Directory</tt> object
     */
    public Directory getDirectory(final String filename) {
        final int lastSlash = filename.lastIndexOf('/');
        if (lastSlash == -1) {
            return getDirectoryForPath("");
        }
        return getDirectoryForPath(filename.substring(0, lastSlash + 1));
    }

    /**
     * @param path
     *            for example "src/net/sf/statcvs/"
     * @return the <tt>Directory</tt> corresponding to <tt>statcvs</tt>
     */
    private Directory getDirectoryForPath(final String path) {
        if (directories.containsKey(path)) {
            return (Directory) directories.get(path);
        }
        final Directory parent = getDirectoryForPath(FileUtils.getParentDirectoryPath(path));
        final Directory newDirectory = parent.createSubdirectory(FileUtils.getDirectoryName(path));
        directories.put(path, newDirectory);
        return newDirectory;
    }

    /**
     * New in StatSVN: We need to have access to FileBuilders after they have been created to populate them with version numbers later on.
     * 
     * @todo Beef up this interface to better encapsulate the data structure.
     * 
     * @return this builder's contained (@link FileBuilder)s.
     */
    public Map getFileBuilders() {
        return fileBuilders;
    }

    /**
     * @see RepositoryFileManager#getLinesOfCode(String)
     */
    public int getLOC(final String filename) throws NoLineCountException {
        if (repositoryFileManager == null) {
            throw new NoLineCountException("no RepositoryFileManager");
        }

        return repositoryFileManager.getLinesOfCode(filename);
    }

    public String getProjectName() {
        return projectName;
    }

    /**
     * @see RepositoryFileManager#getRevision(String)
     */
    public String getRevision(final String filename) throws IOException {
        if (repositoryFileManager == null) {
            throw new IOException("no RepositoryFileManager");
        }
        return repositoryFileManager.getRevision(filename);
    }

    /**
     * Returns the {@link SymbolicName} with the given name or creates it if it does not yet exist.
     * 
     * @param name
     *            the symbolic name's name
     * @return the corresponding symbolic name object
     */
    public SymbolicName getSymbolicName(final String name, final Date date) {
        SymbolicName sym = (SymbolicName) symbolicNames.get(name);

        if (sym != null) {
            return sym;
        } else {
            sym = new SymbolicName(name, date);
            symbolicNames.put(name, sym);

            return sym;
        }
    }

    /**
     * Matches a filename against the include and exclude patterns. If no include pattern was specified, all files will be included. If no exclude pattern was
     * specified, no files will be excluded.
     * 
     * @param filename
     *            a filename
     * @return <tt>true</tt> if the filename matches one of the include patterns and does not match any of the exclude patterns. If it matches an include and
     *         an exclude pattern, <tt>false</tt> will be returned.
     */
    public boolean matchesPatterns(final String filename) {
        if (excludePattern != null && excludePattern.matches(filename)) {
            return false;
        }
        if (includePattern != null) {
            return includePattern.matches(filename);
        }
        return true;
    }

    /**
     * Matches a tag against the tag patterns. 
     * 
     * @param tag
     *            a tag
     * @return <tt>true</tt> if the tag matches the tag pattern.
     */
    public boolean matchesTagPatterns(final String tag) {
        if (tagsPattern != null) {
            return tagsPattern.matcher(tag).matches();
        }
        return false;
    }

    /**
     * New in StatSVN: Updates a particular revision for a file with new line count information. If the file or revision does not exist, action will do nothing.
     * 
     * Necessary because line counts are not given in the log file and hence can only be added in a second pass.
     * 
     * @param filename
     *            the file to be updated
     * @param revisionNumber
     *            the revision number to be updated
     * @param linesAdded
     *            the lines that were added
     * @param linesRemoved
     *            the lines that were removed
     */
    public synchronized void updateRevision(final String filename, final String revisionNumber, final int linesAdded, final int linesRemoved) {
        final FileBuilder fb = (FileBuilder) fileBuilders.get(filename);
        if (fb != null) {
            fb.updateRevision(revisionNumber, linesAdded, linesRemoved);
        }
    }

    /**
     * return only a set of matching tag names (from a list on the command line).
     */
    private SortedSet getMatchingSymbolicNames() {
        final TreeSet result = new TreeSet();
        if (this.tagsPattern == null) {
            return result;
        }
        for (final Iterator it = this.symbolicNames.values().iterator(); it.hasNext();) {
            final SymbolicName sn = (SymbolicName) it.next();
            if (sn.getDate() != null && this.tagsPattern.matcher(sn.getName()).matches()) {
                result.add(sn);
            }
        }
        return result;
    }

    private static final class AuthorAnonymizingProvider {
        private AuthorAnonymizingProvider() {
            // no access
        }

        private static int count = 0;

        static synchronized String getNewName() {
            return "author" + (String.valueOf(++count));
        }

    }
}