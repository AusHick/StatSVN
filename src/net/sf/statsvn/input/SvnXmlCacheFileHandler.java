package net.sf.statsvn.input;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is the SAX parser for the our line count persistence mechanism. It feeds information to (@link net.sf.statsvn.input.LineCountsBuilder).
 * 
 * @author Gunter Mussbacher <gunterm@site.uottawa.ca>
 * 
 * @version $Id: SvnXmlCacheFileHandler.java 351 2008-03-28 18:46:26Z benoitx $
 */
public class SvnXmlCacheFileHandler extends DefaultHandler {
	private static final String FATAL_ERROR_MESSAGE = "Invalid StatSVN cache file.";

	private String lastElement = "";

	private final CacheBuilder cacheBuilder;

	/**
	 * Default constructor
	 * 
	 * @param cacheBuilder
	 *            the cacheBuilder to which to send back line count information.
	 */
	public SvnXmlCacheFileHandler(final CacheBuilder cacheBuilder) {
		this.cacheBuilder = cacheBuilder;
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
	 * Handles the end of an xml element and redirects to the appropriate end* method.
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

		if (eName.equals(CacheConfiguration.CACHE)) {
			endCache();
		} else if (eName.equals(CacheConfiguration.PATH)) {
			endPath();
		} else if (eName.equals(CacheConfiguration.REVISION)) {
			endRevision();
		} else {
			fatalError(FATAL_ERROR_MESSAGE);
		}
	}

	/**
	 * End of line counts element.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endCache() throws SAXException {
		checkLastElement(CacheConfiguration.CACHE);
		lastElement = "";
	}

	/**
	 * End of path element.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endPath() throws SAXException {
		checkLastElement(CacheConfiguration.PATH);
		lastElement = CacheConfiguration.CACHE;
	}

	/**
	 * End of revision element.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endRevision() throws SAXException {
		checkLastElement(CacheConfiguration.PATH);
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
	 * Handles the start of an xml element and redirects to the appropriate start* method.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);

		String eName = localName; // element name
		if ("".equals(eName)) {
			eName = qName; // namespaceAware = false
		}

		if (eName.equals(CacheConfiguration.CACHE)) {
			startCache();
		} else if (eName.equals(CacheConfiguration.PATH)) {
			startPath(attributes);
		} else if (eName.equals(CacheConfiguration.REVISION)) {
			startRevision(attributes);
		} else {
			fatalError(FATAL_ERROR_MESSAGE);
		}
	}

	/**
	 * Handles the start of the document. Initializes the line count builder.
	 * 
	 * @throws SAXException
	 *             unable to build the root.
	 */
	private void startCache() throws SAXException {
		checkLastElement("");
		lastElement = CacheConfiguration.CACHE;
		try {
			cacheBuilder.buildRoot();
		} catch (final ParserConfigurationException e) {
			fatalError(FATAL_ERROR_MESSAGE);
		}
	}

	/**
	 * Handles start of a path. Initializes line count builder for use with this filename.
	 * 
	 * @param attributes
	 *            element's xml attributes.
	 * @throws SAXException
	 *             missing some data.
	 */
	private void startPath(final Attributes attributes) throws SAXException {
		checkLastElement(CacheConfiguration.CACHE);
		lastElement = CacheConfiguration.PATH;
		if (attributes != null && attributes.getValue(CacheConfiguration.NAME) != null) {
			final String name = attributes.getValue(CacheConfiguration.NAME);
			String revision = "0";
			if (attributes.getValue(CacheConfiguration.LATEST_REVISION) != null) {
				revision = attributes.getValue(CacheConfiguration.LATEST_REVISION);
			}
			String binaryStatus = CacheConfiguration.UNKNOWN;
			if (attributes.getValue(CacheConfiguration.BINARY_STATUS) != null) {
				binaryStatus = attributes.getValue(CacheConfiguration.BINARY_STATUS);
			}
			cacheBuilder.buildPath(name, revision, binaryStatus);
		} else {
			fatalError(FATAL_ERROR_MESSAGE);
		}
	}

	/**
	 * Handles start of a revision. Gives information back to the line count builder.
	 * 
	 * @param attributes
	 *            element's xml attributes.
	 * @throws SAXException
	 *             missing some data.
	 */
	private void startRevision(final Attributes attributes) throws SAXException {
		checkLastElement(CacheConfiguration.PATH);
		if (attributes != null && attributes.getValue(CacheConfiguration.NUMBER) != null && attributes.getValue(CacheConfiguration.ADDED) != null
		        && attributes.getValue(CacheConfiguration.REMOVED) != null) {
			final String number = attributes.getValue(CacheConfiguration.NUMBER);
			final String added = attributes.getValue(CacheConfiguration.ADDED);
			final String removed = attributes.getValue(CacheConfiguration.REMOVED);
			String binaryStatus = CacheConfiguration.UNKNOWN;
			if (attributes.getValue(CacheConfiguration.BINARY_STATUS) != null) {
				binaryStatus = attributes.getValue(CacheConfiguration.BINARY_STATUS);
			}
			cacheBuilder.buildRevision(number, added, removed, binaryStatus);
		} else {
			fatalError(FATAL_ERROR_MESSAGE);
		}
	}
}
