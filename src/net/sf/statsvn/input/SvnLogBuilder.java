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
 */
package net.sf.statsvn.input;

import java.util.Map;

/**
 * <p>
 * Interface for defining a Builder that constructs a data structure from a SVM
 * logfile. {@link SvnLogfileParser} takes an instance of this interface and
 * will call methods on the interface for every piece of data it encounters in
 * the log.
 * </p>
 * 
 * <p>
 * First, {@link #buildModule} will be called with the name of the module. Then,
 * {@link #buildFile} will be called with the filename and other pieces of
 * information of the first file in the log. Then, for every revision of this
 * file, {@link #buildRevision} is called. The calls to <tt>buildFile</tt> and
 * <tt>buildRevision</tt> are repeated for every file in the log.
 * </p>
 * 
 * <p>
 * The files are in no particular order. The revisions of one file are ordered
 * by time, beginning with the <em>most recent</em>.
 * </p>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @author Tammo van Lessen
 * @version $Id: SvnLogBuilder.java 351 2008-03-28 18:46:26Z benoitx $
 */
public interface SvnLogBuilder {

	/**
	 * Starts building a module.
	 * 
	 * @param moduleName
	 *            the name of the module
	 */
	void buildModule(String moduleName);

	/**
	 * Starts building a new file. The files are not processed in any particular
	 * order.
	 * 
	 * @param filename
	 *            the file's name with path relative to the module, for example
	 *            "path/file.txt"
	 * @param isBinary
	 *            <tt>true</tt> if it's a binary file
	 * @param isInAttic
	 *            <tt>true</tt> if the file is dead on the main branch
	 * @param revBySymnames
	 *            maps revision (string) by symbolic name (string)
	 * @param dateBySymnames
	 *            maps date (date) by symbolic name (string)
	 */
	void buildFile(String filename, boolean isBinary, boolean isInAttic, Map revBySymnames, final Map dateBySymnames);

	/**
	 * Adds a revision to the last file that was built.. The revisions are added
	 * in SVN logfile order, that is starting with the most recent one.
	 * 
	 * @param data
	 *            the revision
	 */
	void buildRevision(RevisionData data);

	/**
	 * Adds a file to the attic. This method should only be called if our first
	 * invocation to (@link #buildFile(String, boolean, boolean, Map)) was given
	 * an invalid isInAttic field.
	 * 
	 * This is a way to handle post-processing of implicit deletions at the same
	 * time as the implicit additions that can be found in Subversion.
	 * 
	 * @param filename
	 *            the filename to add to the attic.
	 */
	void addToAttic(String filename);

	/**
	 * New in StatSVN: We need to have access to FileBuilders after they have
	 * been created to populate them with version numbers later on.
	 * 
	 * @todo Beef up this interface to better encapsulate the data structure.
	 * 
	 * @return this builder's contained (@link FileBuilder)s.
	 */
	Map getFileBuilders();

	/**
	 * New in StatSVN: Updates a particular revision for a file with new line
	 * count information. If the file or revision does not exist, action will do
	 * nothing.
	 * 
	 * Necessary because line counts are not given in the log file and hence can
	 * only be added in a second pass.
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
	void updateRevision(String filename, String revisionNumber, int linesAdded, int linesRemoved);

	/**
	 * Matches a filename against the include and exclude patterns. If no
	 * include pattern was specified, all files will be included. If no exclude
	 * pattern was specified, no files will be excluded.
	 * 
	 * @param filename
	 *            a filename
	 * @return <tt>true</tt> if the filename matches one of the include
	 *         patterns and does not match any of the exclude patterns. If it
	 *         matches an include and an exclude pattern, <tt>false</tt> will
	 *         be returned.
	 */
	boolean matchesPatterns(final String filename);

	/**
	 * Matches a tag against the tag patterns. 
	 * 
	 * @param tag
	 *            a tag
	 * @return <tt>true</tt> if the tag matches the tag pattern.
	 */
	boolean matchesTagPatterns(final String tag);
}