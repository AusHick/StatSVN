package net.sf.statsvn.input;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.statcvs.output.ConfigurationOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * <p>
 * This class receives information from the (@link net.sf.statsvn.input.SvnXmlRepositoriesFileHandler)
 * to build a DOM-based XML structure containing the names of all repositories and associated line counts xml files.
 * It then allows to retrieve the line counts XML file name for a given repository.
 * </p>
 * 
 * @author Gunter Mussbacher <gunterm@site.uottawa.ca>
 * 
 * @version $Id: RepositoriesBuilder.java 351 2008-03-28 18:46:26Z benoitx $
 * 
 */
public class RepositoriesBuilder {
	private static final String FILE_EXTENSION = ".xml";

	private static final String FILE_PREFIX = "cache_";

	private static final String REPOSITORIES = "repositories";

	private static final String UUID = "uuid";

	private static final String FILE = "file";

	private static final String PROJECT = "project";

	private static final String REPOSITORY = "repository";

	private Document document = null;

	private Element repositories = null;

	/**
	 * Constructs the RepositoriesBuilder
	 * 
	 */
	public RepositoriesBuilder() {
	}

	/**
	 * Finds a repository in the DOM.
	 * 
	 * @param uuid
	 *            the uuid of the repository
	 * @return the repository or null if the repository does not exist
	 */
	private Element findRepository(final String uuid) {
		final NodeList paths = repositories.getChildNodes();
		for (int i = 0; i < paths.getLength(); i++) {
			final Element path = (Element) paths.item(i);
			if (uuid.equals(path.getAttribute(UUID))) {
				return path;
			}
		}
		return null;
	}

	/**
	 * Adds a repository to the DOM structure.
	 * 
	 * @param uuid
	 *            the uuid of the repository
	 * @param file
	 *            the filename for the XML line counts file
	 */
	public Element buildRepository(final String uuid, final String file) {
		final Element repository = document.createElement(REPOSITORY);
		repository.setAttribute(UUID, uuid);
		repository.setAttribute(FILE, file);
		repository.setAttribute(PROJECT, ConfigurationOptions.getProjectName());
		repositories.appendChild(repository);
		return repository;
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
		repositories = document.createElement(REPOSITORIES);
		document.appendChild(repositories);
	}

	/**
	 * Retrieves the file name of the line counts xml file for a given repository.
	 * Creates a new file name if the line counts xml file does not exist.
	 * 
	 * If the repositories xml file does not exist (i.e. the document is null), 
	 * a new document is created.
	 * 
	 * @param uuid
	 *            the uuid of the repository
	 *            
	 * @return the file name or "" if an unexpected error occurs          
	 */
	public String getFileName(final String uuid) {
		if (document == null) {
			try {
				buildRoot();
			} catch (final ParserConfigurationException e) {
				document = null;
			}
		}
		if (document != null) {
			Element repository = findRepository(uuid);
			if (repository == null) {
				repository = buildRepository(uuid, FILE_PREFIX + uuid + FILE_EXTENSION);
			}
			return repository.getAttribute(FILE);
		}
		return "";
	}

	/**
	 * Returns the DOM object when building is complete.
	 * 
	 * @return the DOM document.
	 */
	public Document getDocument() {
		return document;
	}

}
