package net.sf.statsvn.input;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import net.sf.statsvn.output.SvnConfigurationOptions;
import net.sf.statsvn.util.XMLUtil;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is the SAX parser for the svn log in xml format. It feeds information to
 * the (@link net.sf.statsvn.input.SvnLogBuilder).
 * 
 * @author Jason Kealey <jkealey@shade.ca>
 * @author Gunter Mussbacher <gunterm@site.uottawa.ca>
 * 
 * @version $Id: SvnXmlLogFileHandler.java 351 2008-03-28 18:46:26Z benoitx $
 */
public class SvnXmlLogFileHandler extends DefaultHandler {

	private static final String INVALID_SVN_LOG_FILE = "Invalid SVN log file.";

	private static final String AUTHOR = "author";

	private static final String DATE = "date";

	private static final String FATAL_ERROR_MESSAGE = INVALID_SVN_LOG_FILE;

	private static final String LOG = "log";

	private static final String LOGENTRY = "logentry";

	private static final String MSG = "msg";

	private static final String PATH = "path";

	private static final String PATHS = "paths";

	private final SvnLogBuilder builder;

	private ArrayList currentFilenames;

	private RevisionData currentRevisionData;

	private ArrayList currentRevisions;

	private String lastElement = "";

	private String pathAction = "";

	private String stringData = "";

	private String copyfromRev = "";

	private String copyfromPath = "";

	private final RepositoryFileManager repositoryFileManager;

	private final HashMap tagsMap = new HashMap();

	private final HashMap tagsDateMap = new HashMap();

	/**
	 * Default constructor.
	 * 
	 * @param builder
	 *            where to send the information
	 * @param repositoryFileManager
	 *            the repository file manager needed to obtain some information.
	 */
	public SvnXmlLogFileHandler(final SvnLogBuilder builder, final RepositoryFileManager repositoryFileManager) {
		this.builder = builder;
		this.repositoryFileManager = repositoryFileManager;
	}

	/**
	 * Builds the string that was read; default implementation can invoke this
	 * function multiple times while reading the data.
	 */
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
		super.characters(ch, start, length);
		stringData += new String(ch, start, length);
	}

	/**
	 * Makes sure the last element received is appropriate.
	 * 
	 * @param last
	 *            the expected last element.
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void checkLastElement(final String last) throws SAXException {
		if (!lastElement.equals(last)) {
			fatalError(FATAL_ERROR_MESSAGE);
		}
	}

	/**
	 * End of author element. Saves author to the current revision.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endAuthor() throws SAXException {
		checkLastElement(LOGENTRY);
		currentRevisionData.setLoginName(stringData);
	}

	/**
	 * End of date element. See (@link XMLUtil#parseXsdDateTime(String)) for
	 * parsing of the particular datetime format.
	 * 
	 * Saves date to the current revision.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endDate() throws SAXException {
		checkLastElement(LOGENTRY);
		Date dt;
		try {
			dt = XMLUtil.parseXsdDateTime(stringData);
			currentRevisionData.setDate(dt);
		} catch (final ParseException e) {
			warning("Invalid date specified.");
		}
	}

	/**
	 * Handles the end of an xml element and redirects to the appropriate end*
	 * method.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		String eName = localName; // element name
		if ("".equals(eName)) {
			eName = qName; // namespaceAware = false
		}
		if (eName.equals(LOG)) {
			endLog();
		} else if (eName.equals(LOGENTRY)) {
			endLogEntry();
		} else if (eName.equals(AUTHOR)) {
			endAuthor();
		} else if (eName.equals(DATE)) {
			endDate();
		} else if (eName.equals(MSG)) {
			endMsg();
		} else if (eName.equals(PATHS)) {
			endPaths();
		} else if (eName.equals(PATH)) {
			endPath();
		} else {
			fatalError(INVALID_SVN_LOG_FILE);
		}
	}

	/**
	 * End of log element.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endLog() throws SAXException {
		checkLastElement(LOG);
		lastElement = "";
	}

	/**
	 * End of log entry element. For each file that was found, builds the file
	 * and revision in (@link SvnLogBuilder).
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endLogEntry() throws SAXException {
		checkLastElement(LOGENTRY);
		lastElement = LOG;

		for (int i = 0; i < currentFilenames.size(); i++) {
			if (currentFilenames.get(i) == null) {
				continue; // skip files that are not on this branch
			}
			final RevisionData revisionData = (RevisionData) currentRevisions.get(i);
			revisionData.setComment(currentRevisionData.getComment());
			revisionData.setDate(currentRevisionData.getDate());
			revisionData.setLoginName(currentRevisionData.getLoginName());
			final String currentFilename = currentFilenames.get(i).toString();

			final boolean isBinary = repositoryFileManager.isBinary(currentFilename);
			builder.buildFile(currentFilename, isBinary, revisionData.isDeletion(), tagsMap, tagsDateMap);
			builder.buildRevision(revisionData);
		}
	}

	/**
	 * End of msg element. Saves comment to the current revision.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endMsg() throws SAXException {
		checkLastElement(LOGENTRY);
		currentRevisionData.setComment(stringData);
	}

	/**
	 * End of path element. Builds a revision data for this element using the
	 * information that is known to date; rest is done in (@link #endLogEntry())
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endPath() throws SAXException {
		checkLastElement(PATHS);

		// relies on the fact that absoluteToRelativePath returns null for paths
		// that are not on the branch.
		final String filename = repositoryFileManager.absoluteToRelativePath(stringData);
		final RevisionData data = currentRevisionData.createCopy();
		if (!pathAction.equals("D")) {
			data.setStateExp(true);
			if (pathAction.equals("A") || pathAction.equals("R")) {
				data.setStateAdded(true);
			}
		} else {
			data.setStateDead(true);
		}

		final String tagsStr = SvnConfigurationOptions.getTagsDirectory();
		if (copyfromRev != null && filename == null && stringData != null && stringData.indexOf(tagsStr) >= 0) {
			String tag = stringData.substring(stringData.indexOf(tagsStr) + tagsStr.length());
			if (tag.indexOf("/") >= 0) {
				tag = tag.substring(0, tag.indexOf("/"));
			}

			if (!tagsMap.containsKey(tag) && builder.matchesTagPatterns(tag)) {
				SvnConfigurationOptions.getTaskLogger().info("= TAG " + tag + " rev:" + copyfromRev + " stringData [" + stringData + "]");
				tagsMap.put(tag, copyfromRev);
				tagsDateMap.put(tag, currentRevisionData.getDate());
			}
		}

		data.setCopyfromPath(copyfromPath);
		data.setCopyfromRevision(copyfromRev);

		currentRevisions.add(data);
		currentFilenames.add(filename);
	}

	/**
	 * End of paths element.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endPaths() throws SAXException {
		checkLastElement(PATHS);
		lastElement = LOGENTRY;
	}

	/**
	 * Throws a fatal error with the specified message.
	 * 
	 * @param message
	 *            the reason for the error
	 * @throws SAXException
	 *             the error
	 */
	private void fatalError(final String message) throws SAXException {
		fatalError(new SAXParseException(message, null));
	}

	/**
	 * Start of author, date or message.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void startAuthorDateMsg() throws SAXException {
		checkLastElement(LOGENTRY);
	}

	/**
	 * Handles the start of an xml element and redirects to the appropriate
	 * start* method.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		stringData = "";
		String eName = localName; // element name
		if ("".equals(eName)) {
			eName = qName; // namespaceAware = false
		}
		if (eName.equals(LOG)) {
			startLog();
		} else if (eName.equals(LOGENTRY)) {
			startLogEntry(attributes);
		} else if (eName.equals(AUTHOR) || eName.equals(DATE) || eName.equals(MSG)) {
			startAuthorDateMsg();
		} else if (eName.equals(PATHS)) {
			startPaths();
		} else if (eName.equals(PATH)) {
			startPath(attributes);
		} else {
			fatalError(INVALID_SVN_LOG_FILE);
		}
	}

	/**
	 * Start of the log element.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void startLog() throws SAXException {
		checkLastElement("");
		lastElement = LOG;

		try {
			repositoryFileManager.loadInfo();
			builder.buildModule(repositoryFileManager.getModuleName());
		} catch (final Exception e) {
			throw new SAXException(e);
		}

	}

	/**
	 * Start of the log entry element. Initializes information, to be filled
	 * during this log entry and used in (@link #endLogEntry())
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void startLogEntry(final Attributes attributes) throws SAXException {
		checkLastElement(LOG);
		lastElement = LOGENTRY;
		currentRevisionData = new RevisionData();
		currentRevisions = new ArrayList();
		currentFilenames = new ArrayList();
		if (attributes != null && attributes.getValue("revision") != null) {
			currentRevisionData.setRevisionNumber(attributes.getValue("revision"));
		} else {
			fatalError(INVALID_SVN_LOG_FILE);
		}
	}

	/**
	 * Start of the path element. Saves the path action.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void startPath(final Attributes attributes) throws SAXException {
		checkLastElement(PATHS);
		if (attributes != null && attributes.getValue("action") != null) {
			pathAction = attributes.getValue("action");
		} else {
			fatalError(INVALID_SVN_LOG_FILE);
		}

		copyfromPath = attributes.getValue("copyfrom-path");
		copyfromRev = attributes.getValue("copyfrom-rev");

	}

	/**
	 * Start of the paths element.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void startPaths() throws SAXException {
		checkLastElement(LOGENTRY);
		lastElement = PATHS;
	}

	/**
	 * Logs a warning.
	 * 
	 * @param message
	 *            the reason for the error
	 * @throws SAXException
	 *             the error
	 */
	private void warning(final String message) throws SAXException {
		SvnConfigurationOptions.getTaskLogger().info(message);
	}
}
