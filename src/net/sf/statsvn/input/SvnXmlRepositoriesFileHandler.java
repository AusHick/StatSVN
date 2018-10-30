package net.sf.statsvn.input;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is the SAX parser for the repositories xml file. This xml file 
 * identifies the line counts xml files for all repositories for which 
 * SVN stats have been compiled.
 * 
 * @author Gunter Mussbacher <gunterm@site.uottawa.ca>
 * 
 * @version $Id: SvnXmlRepositoriesFileHandler.java 351 2008-03-28 18:46:26Z benoitx $
 */
public class SvnXmlRepositoriesFileHandler extends DefaultHandler {

	private static final String FATAL_ERROR_MESSAGE = "Invalid StatSvn repositories file.";

	private static final String REPOSITORIES = "repositories";

	private static final String REPOSITORY = "repository";

	private static final String UUID = "uuid";

	private static final String FILE = "file";

	private String lastElement = "";

	private final RepositoriesBuilder repositoriesBuilder;

	/**
	 * Default constructor
	 * 
	 * @param repositoriesBuilder
	 *            the RepositoriesBuilder to which to send back the repository information.
	 */
	public SvnXmlRepositoriesFileHandler(final RepositoriesBuilder repositoriesBuilder) {
		this.repositoriesBuilder = repositoriesBuilder;
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

		if (eName.equals(REPOSITORIES)) {
			endRepositories();
		} else if (eName.equals(REPOSITORY)) {
			endRepository();
		} else {
			fatalError(FATAL_ERROR_MESSAGE);
		}
	}

	/**
	 * End of repositories element.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endRepositories() throws SAXException {
		checkLastElement(REPOSITORIES);
		lastElement = "";
	}

	/**
	 * End of repository element.
	 * 
	 * @throws SAXException
	 *             unexpected event.
	 */
	private void endRepository() throws SAXException {
		checkLastElement(REPOSITORY);
		lastElement = REPOSITORIES;
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

		if (eName.equals(REPOSITORIES)) {
			startRepositories();
		} else if (eName.equals(REPOSITORY)) {
			startRepository(attributes);
		} else {
			fatalError(FATAL_ERROR_MESSAGE);
		}
	}

	/**
	 * Handles the start of the document. Initializes the repository builder.
	 * 
	 * @throws SAXException
	 *             unable to build the root.
	 */
	private void startRepositories() throws SAXException {
		checkLastElement("");
		lastElement = REPOSITORIES;
		try {
			repositoriesBuilder.buildRoot();
		} catch (final ParserConfigurationException e) {
			fatalError(FATAL_ERROR_MESSAGE);
		}
	}

	/**
	 * Handles start of a repository. Initializes repositories builder for use with repository.
	 * 
	 * @param attributes
	 *            element's xml attributes.
	 * @throws SAXException
	 *             missing some data.
	 */
	private void startRepository(final Attributes attributes) throws SAXException {
		checkLastElement(REPOSITORIES);
		lastElement = REPOSITORY;
		if (attributes != null && attributes.getValue(UUID) != null && attributes.getValue(FILE) != null) {
			final String uuid = attributes.getValue(UUID);
			final String file = attributes.getValue(FILE);
			repositoriesBuilder.buildRepository(uuid, file);
		} else {
			fatalError(FATAL_ERROR_MESSAGE);
		}
	}

}
