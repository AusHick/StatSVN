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
 
 $RCSfile: RevisionData.java,v $
 $Date: 2004/10/12 07:22:42 $
 */
package net.sf.statsvn.input;

import java.util.Date;

/**
 * Container for all information contained in one SVN revision.
 * 
 * @author Richard Cyganiak <richard@cyganiak.de> *
 * @author Gunter Mussbacher <gunterm@site.uottawa.ca>
 * @author Jason Kealey <jkealey@shade.ca>
 * 
 * @version $Id: RevisionData.java 351 2008-03-28 18:46:26Z benoitx $
 */
public class RevisionData {
	private String revisionNumber;

	private Date date;

	private String loginName;

	private boolean stateExp = false;

	private boolean stateDead = false;

	private boolean stateAdded = false;

	private boolean hasNoLines = true;

	private int linesAdded;

	private int linesRemoved;

	private String comment = "";

	private String copyfromPath;

	private String copyfromRevision;

	public RevisionData() {
	}

	/**
	 * @return Returns the loginName.
	 */
	public String getLoginName() {
		return loginName;
	}

	/**
	 * @param authorName
	 *            The loginName to set.
	 */
	public void setLoginName(final String authorName) {
		this.loginName = authorName;
	}

	/**
	 * @return Returns the date.
	 */
	public Date getDate() {
		return date != null ? new Date(date.getTime()) : null;
	}

	/**
	 * @param date
	 *            The date to set.
	 */
	public void setDate(final Date date) {
		if (date != null) {
			this.date = new Date(date.getTime());
		} else {
			this.date = null;
		}
	}

	/**
	 * @return Returns the linesAdded.
	 */
	public int getLinesAdded() {
		return linesAdded;
	}

	/**
	 * @return Returns the linesRemoved.
	 */
	public int getLinesRemoved() {
		return linesRemoved;
	}

	/**
	 * Checks if the revision contains numbers for the added and removed lines.
	 * 
	 * @return true if the revision contains numbers for the added and removed lines
	 */
	public boolean hasNoLines() {
		return hasNoLines;
	}

	/**
	 * Sets the number of added and removed lines.
	 * 
	 * @param added
	 *            The number of added lines
	 * @param removed
	 *            The number of removed lines
	 */
	public void setLines(final int added, final int removed) {
		this.linesAdded = added;
		this.linesRemoved = removed;
		hasNoLines = false;
	}

	/**
	 * @return Returns the revisionNumber.
	 */
	public String getRevisionNumber() {
		return revisionNumber;
	}

	/**
	 * Sets the revision number.
	 * 
	 * @param revision
	 *            The revision number
	 */
	public void setRevisionNumber(final String revision) {
		this.revisionNumber = revision;
	}

	/**
	 * Is this revision a deletion?
	 * 
	 * @param isDead
	 *            <tt>true</tt> if revision is a deletion.
	 */
	public void setStateDead(final boolean isDead) {
		stateDead = isDead;
	}

	/**
	 * Is the revision exposed. This is CVS speak for any "live" revisionNumber, that is, if this is the current revisionNumber, then a file exists in the
	 * working copy.
	 * 
	 * New in StatSVN: We use it to mean this revision is not a deletion revision. (modify, add or replace)
	 * 
	 * @param isExposed
	 *            <tt>true</tt> true if the revision is not a deletion.
	 */
	public void setStateExp(final boolean isExposed) {
		stateExp = isExposed;
	}

	/**
	 * Is this revision an addition?
	 * 
	 * New in StatSVN: This is no longer a still exists in working copy. We use it to mean this revision is not a deletion revision.
	 * 
	 * @param isAdded
	 */
	public void setStateAdded(final boolean isAdded) {
		stateAdded = isAdded;
	}

	/**
	 * @return Returns the comment.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment
	 *            The comment to set.
	 */
	public void setComment(final String comment) {
		this.comment = comment;
	}

	/**
	 * Returns <tt>true</tt> if this revisionNumber is the removal of a file.
	 * 
	 * @return <tt>true</tt> if this revisionNumber deletes the file.
	 * 
	 */
	public boolean isDeletion() {
		return stateDead;
	}

	/**
	 * Returns <tt>true</tt> if this revisionNumber is a normal change.
	 * 
	 * New in StatSVN: This was isChangeOrRestore before.
	 * 
	 * @return <tt>true</tt> if this is a normal change or a restore.
	 */
	public boolean isChange() {
		// return stateExp && !hasNoLines;
		return stateExp && !stateAdded;
	}

	/**
	 * Returns <tt>true</tt> if this revisionNumber is the creation of a new file or a restore.. The distinction between these two cases can be made by
	 * looking at the previous (in time, not log order) revisionNumber. If it was a deletion, then this revisionNumber is a restore.
	 * 
	 * New in StatSVN: This was isCreation before.
	 * 
	 * @return <tt>true</tt> if this is the creation of a new file.
	 */
	public boolean isCreationOrRestore() {
		// return stateExp && hasNoLines;
		return stateExp && stateAdded;
	}

	/**
	 * Returns <tt>true</tt> if this is an Exp ("exposed"?) revisionNumber. This is CVS speak for any "live" revisionNumber, that is, if this is the current
	 * revisionNumber, then a file exists in the working copy.
	 * 
	 * New in StatSVN: We use it to mean this revision is not a deletion revision. (modify, add or replace)
	 * 
	 * @return <tt>true</tt> if this is an Exp revisionNumber
	 */
	public boolean isStateExp() {
		return stateExp;
	}

	/**
	 * Returns <tt>true</tt> if this is a dead revisionNumber. If this is the current revisionNumber, then the file does not exist in the working copy.
	 * 
	 * @return <tt>true</tt> if this is a dead revisionNumber
	 */
	public boolean isStateDead() {
		return stateDead;
	}

	/**
	 * Returns the current revision data in string format.
	 */
	public String toString() {
		return "RevisionData " + revisionNumber;
	}

	/**
	 * Returns a new instance of the RevisionData, with the same fields as the current one.
	 * 
	 * @return the clone
	 */
	public RevisionData createCopy() {
		final RevisionData copy = new RevisionData(revisionNumber, date, stateExp, stateDead, stateAdded, hasNoLines, linesAdded, linesRemoved);
		copy.setComment(comment);
		copy.setLoginName(loginName);
		return copy;
	}

	/**
	 * Private constructor used by (@link #clone())
	 * 
	 * @param revisionNumber
	 *            the revision number
	 * @param date
	 *            the revision date
	 * @param stateExp
	 *            if this were the current revision, would the file still be live (not-dead)
	 * @param stateDead
	 *            is this a deletion revision
	 * @param stateAdded
	 *            is this an addition revision
	 * @param hasNoLines
	 *            have we set the line counts?
	 * @param linesAdded
	 *            number of lines added
	 * @param linesRemoved
	 *            number of lines removed
	 */
	private RevisionData(final String revisionNumber, final Date date, final boolean stateExp, final boolean stateDead, final boolean stateAdded,
	        final boolean hasNoLines, final int linesAdded, final int linesRemoved) {
		super();
		this.revisionNumber = revisionNumber;
		this.date = date;
		this.stateExp = stateExp;
		this.stateDead = stateDead;
		this.hasNoLines = hasNoLines;
		this.linesAdded = linesAdded;
		this.linesRemoved = linesRemoved;
		this.stateAdded = stateAdded;
	}

	public String getCopyfromPath() {
		return copyfromPath;
	}

	public void setCopyfromPath(final String copyfromPath) {
		this.copyfromPath = copyfromPath;
	}

	public String getCopyfromRevision() {
		return copyfromRevision;
	}

	public void setCopyfromRevision(final String copyfromRevision) {
		this.copyfromRevision = copyfromRevision;
	}

}