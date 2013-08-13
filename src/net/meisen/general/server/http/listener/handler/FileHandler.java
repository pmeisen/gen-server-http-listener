package net.meisen.general.server.http.listener.handler;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.meisen.general.genmisc.exceptions.registry.IExceptionRegistry;
import net.meisen.general.genmisc.types.Files;
import net.meisen.general.server.http.listener.HttpListener;
import net.meisen.general.server.http.listener.api.IHandler;
import net.meisen.general.server.http.listener.exceptions.FileHandlerException;
import net.meisen.general.server.settings.pojos.Extension;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * The <code>FileHandler</code> is used to request files from the file-system.
 * Per default the <code>DEF_DOCROOT</code> is used.
 * 
 * <pre>
 * &lt;!-- uses the DEF_DOCROOT as document-root --&gt;
 * &lt;connector port=&quot;666&quot; listener=&quot;HTTP&quot;/&gt;
 *   &lt;e:extension&gt;
 *     &lt;docroot/&gt;
 *   &lt;/e:extension&gt;
 * &lt;/connector&gt;
 * </pre>
 * 
 * <pre>
 * &lt;!-- uses the defined folder as document-root --&gt;
 * &lt;connector port=&quot;666&quot; listener=&quot;HTTP&quot;&gt;
 *   &lt;e:extension&gt;
 *     &lt;docroot urlmatcher="def/.*"&gt;C:\web-def\&lt;/docroot&gt;
 *     &lt;docroot&gt;C:\web-root\&lt;/docroot&gt;
 *   &lt;/e:extension&gt;
 * &lt;/connector&gt;
 * </pre>
 * 
 * <pre>
 * &lt;!-- define several folders to search in --&gt;
 * &lt;connector port=&quot;666&quot; listener=&quot;HTTP&quot;&gt;
 *   &lt;e:extension&gt;
 *     &lt;docroot urlmatcher="def/.*"&gt;
 *       &lt;location&gt;C:\web-root1\&lt;/location&gt;
 *       &lt;location&gt;C:\web-root2\&lt;/location&gt;
 *     &lt;/docroot&gt;
 *     &lt;docroot&gt;C:\web-root\&lt;/docroot&gt;
 *   &lt;/e:extension&gt;
 * &lt;/connector&gt;
 * </pre>
 * 
 * @see #DEF_DOCROOT
 * 
 * @author pmeisen
 * 
 * 
 */
public class FileHandler implements IHandler {
	private final static Logger LOG = LoggerFactory
			.getLogger(FileHandler.class);

	/**
	 * The extension which can be used to define several locations to look for a
	 * file.
	 */
	public static final String EXTENSION_LOCATION = "location";
	/**
	 * The property used to define the document root of a
	 * <code>FileHandler</code>.
	 */
	public static final String PROPERTY_DOCROOT = "";
	/**
	 * The property to define a comma separated list of default list, e.g.
	 * index.htm, index.html
	 */
	public final static String PROPERTY_DEFFILES = "deffiles";
	/**
	 * The default document-root used when no other is defined.
	 */
	public final static String DEF_DOCROOT = ".";
	/**
	 * The default list of default files
	 */
	public final static String DEF_DEFFILES = "index.htm, index.html";
	/**
	 * The regular expression used to validate the default-filenames
	 */
	public final static String DEFFILE_MATCHER = "(?:[a-zA-Z0-9]+[_\\-]?)+\\.[a-zA-Z0-9]+";

	private List<String> docRoot = null;
	private String prefix = null;
	private List<String> defFileNames = null;

	@Autowired
	@Qualifier("exceptionRegistry")
	private IExceptionRegistry exceptionRegistry;

	@Override
	public void initialize(final Extension e) {

		// determine the document-root to be used
		final List<String> docRoot = determineDocumentRoot(e);

		// validate the docRoot
		validateDocRoot(docRoot);

		// use the document-root
		this.docRoot = new ArrayList<String>();
		for (final String location : docRoot) {
			final String canonicalLocation = Files.getCanonicalPath(location);
			this.docRoot.add(canonicalLocation);
		}

		/*
		 * get the urlMatcher we need that to strip the URI correctly, i.e. if
		 * someone defines the folder 'C:\myTestFolder' to be matched with
		 * '/test/*' the retrieval of http://localhost/test/my/world should try
		 * to retrieve data from 'C:\myTestFolder\my\world' and not
		 * 'C:\myTestFolder\test\my\world'.
		 */
		this.prefix = determinePrefix(e);

		// determine the list of default files
		final List<String> defFiles = determineDefaultFiles(e);

		// validate the default files
		validateDefFiles(defFiles);

		// use the defaultFiles
		this.defFileNames = defFiles;
	}

	/**
	 * Validates the passed <code>defFiles</code> and throws a
	 * <code>FileHandlerException</code> if at least one cannot be validated.
	 * 
	 * @param defFiles
	 *            the files to be validated
	 * 
	 * @throws FileHandlerException
	 *             if at least one of the <code>defFiles</code> cannot be
	 *             validated
	 */
	protected void validateDefFiles(final List<String> defFiles)
			throws FileHandlerException {
		if (defFiles == null) {
			return;
		} else if (defFiles.size() > 0) {
			for (final String defFile : defFiles) {
				// check the file-syntax
				if (!defFile.matches(DEFFILE_MATCHER)) {
					exceptionRegistry.throwException(
							FileHandlerException.class, 1002, defFile,
							DEFFILE_MATCHER);
				}
			}
		}
	}

	/**
	 * Determines the default files defined by the <code>Extension</code>.
	 * 
	 * @param e
	 *            the <code>Extension</code> to determine the default files from
	 * 
	 * @return the determined default files
	 */
	protected List<String> determineDefaultFiles(final Extension e) {
		String defFilesList;

		if (e == null) {
			defFilesList = DEF_DEFFILES;
		} else if ((defFilesList = e.<String> getProperty(PROPERTY_DEFFILES)) != null) {
			// nothing to do
		} else {
			defFilesList = DEF_DEFFILES;
		}

		// get the list
		final List<String> list;
		if (defFilesList == null || "".equals(defFilesList.trim())) {
			list = new ArrayList<String>();
		} else {
			list = Arrays.asList(defFilesList.split("\\s*,\\s*"));
		}

		return list;
	}

	/**
	 * Determines the prefix, which should be removed from the URI, prior to
	 * looking up the file to be requested from the file-system.
	 * 
	 * @param e
	 *            the <code>Extension</code> to read the prefix from
	 * 
	 * @return the determined prefix
	 */
	protected String determinePrefix(final Extension e) {
		String prefix;
		if (e == null) {
			prefix = "";
		} else if ((prefix = e
				.<String> getProperty(HttpListener.PROPERTY_URLMATCHER)) != null) {

			final int pos = prefix.lastIndexOf("/");
			if (pos == -1) {
				prefix = "";
			} else {
				prefix = prefix.substring(0, pos + 1);
			}
		} else {
			prefix = "";
		}

		return prefix;
	}

	/**
	 * Determines the document-root based on the specified
	 * <code>Extension</code>. The returned document-root shouldn't be validated
	 * within here, this is done in a separate method (
	 * {@link #validateDocRoot(File)}). A document-root can be defined by
	 * several locations, which are searched in order of definition, whereby the
	 * first-location will always be the value defined by the property (i.e.
	 * within the docroot-element).
	 * 
	 * @param e
	 *            the <code>Extension</code> to determine the document-root from
	 * 
	 * @return the not validated document-root
	 * 
	 * @see #validateDocRoot(File)
	 */
	private List<String> determineDocumentRoot(final Extension e) {

		final List<String> docRoot = new ArrayList<String>();

		// make sure that we have an extension for further processing
		if (e == null) {
			docRoot.add(DEF_DOCROOT);
			return docRoot;
		}

		// look up the default location (normally defined that way)
		final String defLocation = e.<String> getProperty(PROPERTY_DOCROOT);
		if (defLocation != null && !"".equals(defLocation.trim())) {
			docRoot.add(defLocation);
		}

		// add all defined locations
		if (e.hasExtension(EXTENSION_LOCATION)) {
			final List<Extension> locExtensions = e
					.getExtensions(EXTENSION_LOCATION);
			for (final Extension locExtension : locExtensions) {
				final String location = locExtension
						.<String> getProperty(PROPERTY_DOCROOT);

				// add the location if one is defined
				if (location == null || "".equals(location.trim())) {
					exceptionRegistry.throwException(
							FileHandlerException.class, 1003,
							EXTENSION_LOCATION);
				} else {
					docRoot.add(location);
				}
			}
		}

		// check if we found something
		if (docRoot.size() == 0) {
			docRoot.add(DEF_DOCROOT);
		}

		return docRoot;
	}

	/**
	 * Validates the passed file, to be used as document-root. The method should
	 * throw a <code>FileHandlerException</code> if the validation is fails.
	 * 
	 * @param docRoot
	 *            the document-root to be validated
	 * 
	 * @throws FileHandlerException
	 *             if the validation fails
	 */
	protected void validateDocRoot(final List<String> docRoot)
			throws FileHandlerException {

		if (docRoot == null) {
			exceptionRegistry.throwException(FileHandlerException.class, 1001);
		}

		for (final String location : docRoot) {
			final File file = new File(location);

			if (!file.exists() || !file.canRead()) {
				exceptionRegistry.throwException(FileHandlerException.class,
						1000, file.getName());
			}
		}
	}

	/**
	 * Determines the file to be used for the specified <code>uri</code>.
	 * 
	 * @param uri
	 *            the uri to determine the <code>File</code> for
	 * 
	 * @return the <code>File</code> the passed <code>uri</code> points to
	 * 
	 * @throws IOException
	 *             if the <code>uri</code> cannot be decoded
	 */
	protected File determineFile(final String uri) throws IOException {

		// there is no empty file
		if (uri == null || "".equals(uri.trim())) {
			return null;
		}

		// first decode it
		String decUri = URLDecoder.decode(uri, "UTF-8");
		if (decUri.startsWith(prefix)) {
			decUri = decUri.substring(prefix.length());

			// we search for the file and for a default file
			File file = null;
			File defFile = null;

			// search in each location if we have the file
			for (final String location : docRoot) {
				File locationFile = new File(location);

				/*
				 * check if a file is specified which we can use
				 */
				if (!locationFile.exists() || !locationFile.canRead()) {
					continue;
				}
				/*
				 * if we have a file directly we are done
				 */
				else if (locationFile.isFile()) {
					file = locationFile;
				}
				/*
				 * check if we can find the specified file (via URI) in the
				 * directory
				 */
				else {
					locationFile = new File(locationFile, decUri);
					if (locationFile.isFile()) {
						file = locationFile;
						break;
					}
					/*
					 * if we didn't find any default yet, we look it up in the
					 * directory
					 */
					else if (defFile == null && defFileNames != null
							&& locationFile.isDirectory()) {
						for (final String defFiletName : defFileNames) {
							final File defFilePath = new File(locationFile,
									defFiletName);

							// check if the defFilePath is a valid file
							if (defFilePath.exists() && defFilePath.canRead()
									&& defFilePath.isFile()) {
								defFile = defFilePath;
							}
						}
					}
				}
			}

			return file == null ? defFile : file;
		} else {
			return null;
		}

	}

	@Override
	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {

		// check if it has been initialized
		if (docRoot == null) {
			exceptionRegistry.throwException(FileHandlerException.class, 1001);
		}

		// determine the called method for the request
		final String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);
		if (!method.equals("GET") && !method.equals("HEAD")
				&& !method.equals("POST")) {
			throw new MethodNotSupportedException(method
					+ " method not supported");
		}

		// get the target of the request
		final String target = request.getRequestLine().getUri();
		final File file = determineFile(target);
		if (file == null || !file.exists()) {
			final String failedFile = new File(target).getPath();

			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			final StringEntity entity = new StringEntity("<html><body><h1>File"
					+ failedFile + " not found</h1></body></html>",
					ContentType.create("text/html", "UTF-8"));
			response.setEntity(entity);

			if (LOG.isInfoEnabled()) {
				LOG.info("File " + failedFile + " not found");
			}
		} else if (!file.canRead() || file.isDirectory()) {

			response.setStatusCode(HttpStatus.SC_FORBIDDEN);
			final StringEntity entity = new StringEntity(
					"<html><body><h1>Access denied</h1></body></html>",
					ContentType.create("text/html", "UTF-8"));
			response.setEntity(entity);

			if (LOG.isWarnEnabled()) {
				LOG.warn("Cannot read file " + file.getPath());
			}
		} else {
			response.setStatusCode(HttpStatus.SC_OK);

			// get the mime of the file and response with it
			final String mimeType = Files.getMimeType(file);
			final ContentType contentType = ContentType.create(mimeType);
			final FileEntity body = new FileEntity(file, contentType);

			response.setEntity(body);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Serving file " + file.getPath() + " of type "
						+ mimeType);
			}
		}
	}
}
