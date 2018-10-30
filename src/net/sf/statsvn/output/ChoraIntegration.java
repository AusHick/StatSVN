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
 
 $RCSfile: ChoraIntegration.java,v $
 $Date: 2004/10/12 07:22:42 $ 
 */
package net.sf.statsvn.output;

import java.util.Set;

import net.sf.statcvs.model.Directory;
import net.sf.statcvs.model.Revision;
import net.sf.statcvs.model.VersionedFile;
import net.sf.statcvs.output.WebRepositoryIntegration;

/**
 * Integration of the <a href="http://www.horde.org/chora/">Chora CVS Viewer</a>
 * 
 * @author Richard Cyganiak
 * @version $Id: ChoraIntegration.java,v 1.9 2004/10/12 07:22:42 cyganiak Exp $
 */
public class ChoraIntegration implements WebRepositoryIntegration {
	private String baseURL;

	/**
	 * @param baseURL
	 *            base URL of the Chora installation
	 */
	public ChoraIntegration(final String baseURL) {
		if (baseURL.endsWith("/")) {
			this.baseURL = baseURL.substring(0, baseURL.length() - 1);
		} else {
			this.baseURL = baseURL;
		}
	}

	/**
	 * @see net.sf.statsvn.output.WebRepositoryIntegration#getName
	 */
	public String getName() {
		return "Chora";
	}

	/**
	 * @see net.sf.statsvn.output.WebRepositoryIntegration#getDirectoryUrl
	 */
	public String getDirectoryUrl(final Directory directory) {
		return baseURL + "/?f=" + directory.getPath();
	}

	/**
	 * @see net.sf.statsvn.output.WebRepositoryIntegration#getFileHistoryUrl
	 */
	public String getFileHistoryUrl(final VersionedFile file) {
		// chora doesn't seem to support deleted files for subversion
		// repositories
		//		if (isInAttic(file)) {
		// String path = file.getDirectory().getPath();
		// String filename = file.getFilename();
		// return baseURL + "/" + path + "Attic/" + filename;
		//		}
		return this.baseURL + "/?f=" + file.getFilenameWithPath();
	}

	private String getFileViewBaseUrl(final VersionedFile file) {
		return this.baseURL + "/co.php?f=" + file.getFilenameWithPath();
	}

	/**
	 * @see net.sf.statsvn.output.WebRepositoryIntegration#getFileViewUrl(VersionedFile)
	 */
	public String getFileViewUrl(final VersionedFile file) {
		return getFileViewBaseUrl(file) + "&r=HEAD";
	}

	/**
	 * @see net.sf.statsvn.output.WebRepositoryIntegration#getFileViewUrl(VersionedFile)
	 */
	public String getFileViewUrl(final Revision revision) {
		return getFileViewBaseUrl(revision.getFile()) + "&r=" + revision.getRevisionNumber();
	}

	/**
	 * @see net.sf.statsvn.output.WebRepositoryIntegration#getDiffUrl
	 */
	public String getDiffUrl(final Revision oldRevision, final Revision newRevision) {
		if (!oldRevision.getFile().equals(newRevision.getFile())) {
			throw new IllegalArgumentException("revisions must be of the same file");
		}

		return this.baseURL + "/diff.php?f=" + oldRevision.getFile().getFilenameWithPath() + "&r1=" + oldRevision.getRevisionNumber() + "&r2="
		        + newRevision.getRevisionNumber() + "&ty=h";
	}

	/**
	 * @see net.sf.statsvn.output.WebRepositoryIntegration#setAtticFileNames(java.util.Set)
	 */
	public void setAtticFileNames(final Set atticFileNames) {
		//		this.atticFileNames = atticFileNames;
	}

	public String getBaseUrl() {
		return baseURL;
	}
}
