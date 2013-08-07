package net.meisen.general.server.http.listener.handler;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Locale;

import net.meisen.general.genmisc.exceptions.registry.IExceptionRegistry;
import net.meisen.general.genmisc.types.Files;
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
	 * The default document-root used when no other is defined.
	 */
	public final static String DEF_DOCROOT = ".";

	private String docRoot = null;

	@Autowired
	@Qualifier("exceptionRegistry")
	private IExceptionRegistry exceptionRegistry;

	@Override
	public void initialize(final Extension e) {

		// determine the document-root to be used
		String docRoot;

		// if no extension is defined we use the default
		if (e == null) {
			docRoot = DEF_DOCROOT;
		}
		// if the extension has no text-property defined
		else if ((docRoot = e.<String> getProperty("")) != null) {
			docRoot = docRoot.trim();
			docRoot = "".equals(docRoot) ? DEF_DOCROOT : docRoot;
		}
		// we don't have any text-attribute defined
		else {
			docRoot = DEF_DOCROOT;
		}

		// validate the docRoot
		final File docRootFile = new File(docRoot);
		if (!docRootFile.exists() || !docRootFile.canRead()) {
			exceptionRegistry.throwException(FileHandlerException.class, 1000,
					docRoot);
		} else if (!docRootFile.isDirectory()) {
			exceptionRegistry.throwException(FileHandlerException.class, 1002,
					docRoot);
		}

		this.docRoot = Files.getCanonicalPath(docRoot);
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
		final File file = new File(this.docRoot, URLDecoder.decode(target,
				"UTF-8"));
		if (!file.exists()) {

			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			StringEntity entity = new StringEntity("<html><body><h1>File"
					+ file.getPath() + " not found</h1></body></html>",
					ContentType.create("text/html", "UTF-8"));
			response.setEntity(entity);

			if (LOG.isInfoEnabled()) {
				LOG.info("File " + file.getPath() + " not found");
			}
		} else if (!file.canRead() || file.isDirectory()) {

			response.setStatusCode(HttpStatus.SC_FORBIDDEN);
			StringEntity entity = new StringEntity(
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
