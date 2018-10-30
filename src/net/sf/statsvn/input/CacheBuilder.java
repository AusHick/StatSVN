package net.sf.statsvn.input;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.statcvs.output.ConfigurationOptions;
import net.sf.statsvn.output.SvnConfigurationOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * <p>
 * CVS log files include lines modified for each commit and binary status of a
 * file while SVN log files do not offer this additional information.
 * </p>
 * 
 * <p>
 * StatSVN must query the Subversion repository for line counts using svn diff.
 * However, this is very costly, performance-wise. Therefore, the decision was
 * taken to persist this information in an XML file. This class receives
 * information from (@link net.sf.statsvn.input.SvnXmlLineCountsFileHandler) to
 * build a DOM-based xml structure. It also forwards line counts to the
 * appropriate (@link net.sf.statsvn.input.FileBuilder).
 * </p>
 * 
 * @author Gunter Mussbacher <gunterm@site.uottawa.ca>
 * @version $Id: CacheBuilder.java 351 2008-03-28 18:46:26Z benoitx $
 */
public class CacheBuilder {
	private final SvnLogBuilder builder;

	private final RepositoryFileManager repositoryFileManager;

	private Element currentPath = null;

	private Document document = null;

	private String currentFilename;

	private Element cache = null;

	/**
	 * Constructs the LineCountsBuilder by giving it a reference to the builder
	 * currently in use.
	 * 
	 * @param builder
	 *            the SvnLogBuilder which contains all the FileBuilders.
	 */
	public CacheBuilder(final SvnLogBuilder builder, final RepositoryFileManager repositoryFileManager) {
		this.builder = builder;
		this.repositoryFileManager = repositoryFileManager;
	}

	/**
	 * Adds a path in the DOM. To be followed by invocations to (@link
	 * #addRevision(String, String, String))
	 * 
	 * @param name
	 *            the filename
	 * @param latestRevision
	 *            the latest revision of the file for which the binary status is
	 *            known
	 * @param binaryStatus
	 *            binary status of latest revision
	 */
	private void addDOMPath(final String name, final String latestRevision, final String binaryStatus) {
		currentPath = document.createElement(CacheConfiguration.PATH);
		currentPath.setAttribute(CacheConfiguration.NAME, name);
		currentPath.setAttribute(CacheConfiguration.LATEST_REVISION, latestRevision);
		currentPath.setAttribute(CacheConfiguration.BINARY_STATUS, binaryStatus);
		cache.appendChild(currentPath);
	}

	/**
	 * Updates the BINARY_STATUS and LATEST_REVISION attributes of a path in the
	 * DOM. Updates only if the revisionNumber is higher than current
	 * LATEST_REVISION of the path.
	 * 
	 * @param path
	 *            the path to be updated
	 * @param isBinary
	 *            indicates if the revision is binary or not
	 * @param revisionNumber
	 *            the revision number for which the binary status is valid
	 */
	private void updateDOMPath(final Element path, final boolean isBinary, final String revisionNumber) {
		int oldRevision = 0;
		int newRevision = -1;
		try {
			oldRevision = Integer.parseInt(path.getAttribute(CacheConfiguration.LATEST_REVISION));
			newRevision = Integer.parseInt(revisionNumber);
		} catch (final NumberFormatException e) {
			SvnConfigurationOptions.getTaskLogger().log(
			        "Ignoring invalid revision number " + revisionNumber + " for " + path.getAttribute(CacheConfiguration.NAME));
			newRevision = -1;
		}
		String binaryStatus = CacheConfiguration.NOT_BINARY;
		if (isBinary) {
			binaryStatus = CacheConfiguration.BINARY;
		}
		if (newRevision >= oldRevision) {
			path.setAttribute(CacheConfiguration.LATEST_REVISION, revisionNumber);
			path.setAttribute(CacheConfiguration.BINARY_STATUS, binaryStatus);
		}
	}

	/**
	 * Finds a path in the DOM.
	 * 
	 * @param name
	 *            the filename
	 * @return the path or null if the path does not exist
	 */
	private Element findDOMPath(final String name) {
		if (currentPath != null && name.equals(currentPath.getAttribute(CacheConfiguration.NAME))) {
			return currentPath;
		}
		final NodeList paths = cache.getChildNodes();
		for (int i = 0; i < paths.getLength(); i++) {
			final Element path = (Element) paths.item(i);
			if (name.equals(path.getAttribute(CacheConfiguration.NAME))) {
				return path;
			}
		}
		return null;
	}

	/**
	 * Adds a revision to the current path in the DOM. To be preceeded by (@link
	 * #addPath(String))
	 * 
	 * @param number
	 *            the revision number
	 * @param added
	 *            the number of lines that were added
	 * @param removed
	 *            the number of lines that were removed
	 */
	private void addDOMRevision(final String number, final String added, final String removed, final String binaryStatus) {
		final Element revision = document.createElement(CacheConfiguration.REVISION);
		revision.setAttribute(CacheConfiguration.NUMBER, number);
		revision.setAttribute(CacheConfiguration.ADDED, added);
		revision.setAttribute(CacheConfiguration.REMOVED, removed);
		revision.setAttribute(CacheConfiguration.BINARY_STATUS, binaryStatus);
		currentPath.appendChild(revision);
	}

	/**
	 * Initializes the builder for subsequent invocations of (@link
	 * #buildRevision(String, String, String)).
	 * 
	 * @param name
	 *            the filename
	 */
	public void buildPath(final String name, final String revision, final String binaryStatus) {
		currentFilename = repositoryFileManager.absoluteToRelativePath(name);
		addDOMPath(name, revision, binaryStatus);

	}

	/**
	 * Given the file specified by the preceeding invocation to (@link
	 * #buildPath(String)), set the line counts for the given revision.
	 * 
	 * If the path given in the preceeding invocation to (@link
	 * #buildPath(String)) is not used by the (@link SvnLogBuilder), this call
	 * does nothing.
	 * 
	 * @param number
	 *            the revision number
	 * @param added
	 *            the number of lines added
	 * @param removed
	 *            the number of lines removed.
	 */
	public void buildRevision(final String number, final String added, final String removed, final String binaryStatus) {
		if (!added.equals("-1") && !removed.equals("-1")) {
			addDOMRevision(number, added, removed, binaryStatus);
			builder.updateRevision(currentFilename, number, Integer.parseInt(added), Integer.parseInt(removed));
		}
	}

	/**
	 * Builds the DOM root.
	 * 
	 * @throws ParserConfigurationException
	 */
	public void buildRoot() throws ParserConfigurationException {
		final DocumentBuilderFactory factoryDOM = DocumentBuilderFactory.newInstance();
		DocumentBuilder builderDOM;
		builderDOM = factoryDOM.newDocumentBuilder();
		document = builderDOM.newDocument();
		cache = document.createElement(CacheConfiguration.CACHE);
		cache.setAttribute(CacheConfiguration.PROJECT, ConfigurationOptions.getProjectName());
		cache.setAttribute(CacheConfiguration.XML_VERSION, "1.0");
		document.appendChild(cache);
	}

	/**
	 * Returns the DOM object when building is complete.
	 * 
	 * @return the DOM document.
	 */
	public Document getDocument() {
		return document;
	}

	/**
	 * Adds a revision to the DOM.
	 * 
	 * Encapsulates calls to (@link #buildRoot()), (@link #buildPath(String)),
	 * and (@link #buildRevision(String, String, String)) into one easy to use
	 * interface.
	 * 
	 * 
	 * @param name
	 *            the filename
	 * @param number
	 *            the revision number
	 * @param added
	 *            the number of lines added
	 * @param removed
	 *            the number of lines removed
	 */
	public synchronized void newRevision(String name, final String number, final String added, final String removed, final boolean binaryStatus) {
		name = repositoryFileManager.relativeToAbsolutePath(name);
		checkDocument();
		if (document != null) {
			currentPath = findDOMPath(name);
			if (currentPath == null) {
				// changes currentPath to new one
				addDOMPath(name, "0", CacheConfiguration.UNKNOWN);
			}
			String sBinaryStatus = CacheConfiguration.NOT_BINARY;
			if (binaryStatus) {
				sBinaryStatus = CacheConfiguration.BINARY;
			}
			addDOMRevision(number, added, removed, sBinaryStatus);
		}
	}

	private void checkDocument() {
		if (document == null) {
			try {
				buildRoot();
			} catch (final ParserConfigurationException e) {
				document = null;
			}
		}
	}

	/**
	 * Updates all paths in the DOM structure with the latest binary status
	 * information from the working folder.
	 * 
	 * @param name
	 *            the filename
	 * @param number
	 *            the revision number
	 * @param added
	 *            the number of lines added
	 * @param removed
	 *            the number of lines removed
	 */
	public void updateBinaryStatus(final Collection fileBuilders, final String revisionNumber) {
		// change data structure to a more appropriate one for lookup
		final Map mFileBuilders = new HashMap();
		for (final Iterator iter = fileBuilders.iterator(); iter.hasNext();) {
			final FileBuilder fileBuilder = (FileBuilder) iter.next();
			mFileBuilders.put(fileBuilder.getName(), fileBuilder);
		}
		if (!mFileBuilders.isEmpty()) {
			// go through all the paths in the DOM and update their binary
			// status
			// remove the fileBuilder once its corresponding path in the DOM was
			// dealt with
			checkDocument();
			final NodeList paths = cache.getChildNodes();
			for (int i = 0; i < paths.getLength(); i++) {
				final Element path = (Element) paths.item(i);
				if (mFileBuilders.containsKey(repositoryFileManager.absoluteToRelativePath(path.getAttribute(CacheConfiguration.NAME)))) {
					final FileBuilder fileBuilder = (FileBuilder) mFileBuilders.get(repositoryFileManager.absoluteToRelativePath(path
					        .getAttribute(CacheConfiguration.NAME)));
					updateDOMPath(path, fileBuilder.isBinary(), revisionNumber);
					mFileBuilders.remove(repositoryFileManager.absoluteToRelativePath(path.getAttribute(CacheConfiguration.NAME)));
				}
			}
			// go through remaining fileBuilders and add them to the DOM
			final Collection cFileBuilders = mFileBuilders.values();
			for (final Iterator iter = cFileBuilders.iterator(); iter.hasNext();) {
				final FileBuilder fileBuilder = (FileBuilder) iter.next();
				String binaryStatus = CacheConfiguration.NOT_BINARY;
				if (fileBuilder.isBinary()) {
					binaryStatus = CacheConfiguration.BINARY;
				}
				addDOMPath(repositoryFileManager.relativeToAbsolutePath(fileBuilder.getName()), revisionNumber, binaryStatus);
			}
		}

	}

	/**
	 * Checks the path's cached binary status.
	 * 
	 * @param fileName
	 *            the path to be checked
	 * @param revisionNumber
	 *            the revision of the path to be checked
	 * @return true if the path's BINARY_STATUS is true and the revisionNumber
	 *         is lower or equal to the path's LATEST_REVISION
	 */
	public synchronized boolean isBinary(final String fileName, final String revisionNumber) {
		int latestRevision = 0;
		int revisionToCheck = -1;
		checkDocument();
		final Element path = findDOMPath(repositoryFileManager.relativeToAbsolutePath(fileName));
		if (path == null) {
			return false;
		}
		try {
			latestRevision = Integer.parseInt(path.getAttribute(CacheConfiguration.LATEST_REVISION));
			revisionToCheck = Integer.parseInt(revisionNumber);
		} catch (final NumberFormatException e) {
			SvnConfigurationOptions.getTaskLogger().log(
			        "Ignoring invalid revision number " + revisionNumber + " for " + path.getAttribute(CacheConfiguration.NAME));
			revisionToCheck = -1;
		}
		if (latestRevision >= revisionToCheck) {
			final String binaryStatus = path.getAttribute(CacheConfiguration.BINARY_STATUS);
			if (binaryStatus.equals(CacheConfiguration.BINARY)) {
				return true;
			}
		}
		return false;
	}
}
