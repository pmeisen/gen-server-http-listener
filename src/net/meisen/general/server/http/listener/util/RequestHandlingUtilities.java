package net.meisen.general.server.http.listener.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;

/**
 * Utility class when working with <code>HttpRequest</code> instances.
 * 
 * @author pmeisen
 * 
 */
public class RequestHandlingUtilities {
	/**
	 * Default separator for parameters
	 */
	protected static final String PARAMETER_SEPARATOR = "&";
	/**
	 * Default separator for key and value
	 */
	protected static final String NAME_VALUE_SEPARATOR = "=";
	/**
	 * Cookie separator for parameters
	 */
	protected static final String COOKIE_NAME_VALUE_SEPARATOR = "=";
	/**
	 * Cookie separator for key and value
	 */
	protected static final String COOKIE_PARAMETER_SEPARATOR = ";";

	/**
	 * The Class RequestWrapper.
	 */
	private static class RequestWrapper implements RequestContext {

		/**
		 * The request.
		 */
		private final HttpEntityEnclosingRequest request;

		/**
		 * Instantiates a new request wrapper.
		 * 
		 * @param request
		 *            the {@code HttpEntityEnclosingRequest} of the file upload
		 */
		public RequestWrapper(final HttpEntityEnclosingRequest request) {
			this.request = request;
		}

		/**
		 * Implementation of the {@code getCharacterEncoding()} method.
		 * 
		 * @see RequestContext#getCharacterEncoding()
		 */
		public String getCharacterEncoding() {
			return request.getEntity().getContentEncoding() == null ? "ISO-8859-1"
					: request.getEntity().getContentEncoding().getValue();
		}

		/**
		 * Implementation of the {@code getContentLength()} method.
		 */
		public int getContentLength() {
			return (int) request.getEntity().getContentLength();
		}

		/**
		 * Implementation of the {@code getCharacterEncoding()} method.
		 * 
		 * @see RequestContext#getContentType()
		 */
		public String getContentType() {
			return request.getEntity().getContentType().getValue();
		}

		/**
		 * Implementation of the {@code getCharacterEncoding()} method.
		 * 
		 * @see RequestContext#getInputStream()
		 */
		public InputStream getInputStream() throws IOException {
			return ((BasicHttpEntity) request.getEntity()).getContent();
		}
	}

	/**
	 * Get the header information from the request.
	 * 
	 * @param request
	 *            the request to read the header from
	 * 
	 * @return the read header values
	 */
	public static Map<String, String> parseHeaders(final HttpRequest request) {
		final Map<String, String> parameters = new HashMap<String, String>();

		for (final Header header : request.getAllHeaders()) {
			parameters.put(header.getName(), header.getValue());
		}

		return parameters;
	}

	/**
	 * Parses the cookies send via the request-header.
	 * 
	 * @param request
	 *            the request to get the cookie values from
	 * 
	 * @return the read cookie values
	 */
	public static Map<String, String> parseCookies(final HttpRequest request) {
		final Map<String, String> cookies = new HashMap<String, String>();

		for (final Header header : request.getHeaders("Cookie")) {
			cookies.putAll(scanRawForParameters(header.getValue(),
					COOKIE_PARAMETER_SEPARATOR, COOKIE_NAME_VALUE_SEPARATOR));
		}

		return cookies;
	}

	/**
	 * Get the parameters of a <code>HttpRequest</code>.
	 * 
	 * @param request
	 *            the <code>HttpRequest</code> to parse for parameters
	 * 
	 * @return the found parameters, can never be <code>null</code>
	 */
	public static Map<String, String> parseParameter(final HttpRequest request) {
		final Map<String, String> parameters = new HashMap<String, String>();

		final String method = request.getRequestLine().getMethod();

		// get parameters can always be there
		parameters.putAll(parseGetParameter(request));

		// add post parameters
		if ("POST".equals(method)) {
			parameters.putAll(parsePostParameter(request));
		}

		return parameters;
	}

	/**
	 * Parse all the post parameters from the request. The request stream is
	 * closed after reading.
	 * 
	 * @param request
	 *            the {@code HttpRequest} to read from
	 * 
	 * @return the read parameters
	 */
	public static Map<String, String> parsePostParameter(
			final HttpRequest request) {

		// check the request
		if (request == null) {
			return new HashMap<String, String>();
		}

		// For some reason, just putting the incoming entity into
		// the response will not work. We have to buffer the message.
		byte[] data;
		if (request instanceof HttpEntityEnclosingRequest) {
			final HttpEntity entity = ((HttpEntityEnclosingRequest) request)
					.getEntity();
			try {
				data = EntityUtils.toByteArray(entity);
			} catch (final IOException e) {
				data = new byte[0];
			}
		} else {
			data = new byte[0];
		}

		return scanRawForParameters(new String(data));
	}

	/**
	 * Parse all the get parameters from the request
	 * 
	 * @param request
	 *            the {@code HttpRequest} to read from
	 * 
	 * @return the read parameters
	 */
	public static Map<String, String> parseGetParameter(
			final HttpRequest request) {

		// check the request
		if (request == null) {
			return new HashMap<String, String>();
		}

		// get the URI
		final URI uri;
		try {
			uri = new URI(request.getRequestLine().getUri());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Unexpected exception thrown", e);
		}

		// check the uri and it's raw query
		final String rawQuery;
		if (uri == null || (rawQuery = uri.getRawQuery()) == null) {
			return new HashMap<String, String>();
		}

		// work with the retrieved URI
		return scanRawForParameters(rawQuery);
	}

	/**
	 * Handles a file uploaded contained in the specified {@code request}. The
	 * method uses a {@code DiskFileItemFactory} with a {@code sizeThreshold}
	 * set to {@code 0}. If you use FileUpload, please make sure to add the
	 * needed dependencies, i.e. {@code commons-fileupload} and
	 * {@code servlet-api} (later will be removed in a newer version of the
	 * {@code commons-fileupload}.
	 * 
	 * @param request
	 *            the request to handle the file upload from
	 * 
	 * @return the {@code List} of loaded files
	 * 
	 * @throws FileUploadException
	 *             if the file upload fails
	 * 
	 * @see DiskFileItemFactory
	 */
	public static List<FileItem> handleFileUpload(final HttpRequest request)
			throws FileUploadException {
		final DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(0);
		return handleFileUpload(request, factory);
	}

	/**
	 * Handles a file contained in the specified {@code request}. If you use
	 * FileUpload, please make sure to add the needed dependencies, i.e.
	 * {@code commons-fileupload} and {@code servlet-api} (later will be removed
	 * in a newer version of the {@code commons-fileupload}.
	 * 
	 * @param request
	 *            the request to handle the file upload from
	 * @param factory
	 *            the {@code FileItemFactory} used to handle the request
	 * 
	 * @return the {@code List} of loaded files
	 * 
	 * @throws FileUploadException
	 *             if the file upload fails
	 */
	public static List<FileItem> handleFileUpload(final HttpRequest request,
			final FileItemFactory factory) throws FileUploadException {
		final List<FileItem> items;
		if (request instanceof HttpEntityEnclosingRequest
				&& "POST".equals(request.getRequestLine().getMethod())) {

			// create the ServletFileUpload
			final FileUpload upload = new FileUpload(factory);

			final HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
			items = upload.parseRequest(new RequestWrapper(entityRequest));
		} else {
			items = new ArrayList<FileItem>();
		}

		return items;
	}

	/**
	 * Scans the passed {@code raw} string for parameters. Using
	 * {@link #PARAMETER_SEPARATOR} and {@link #NAME_VALUE_SEPARATOR} ass
	 * separators.
	 * 
	 * @param raw
	 *            the string to be parsed
	 * 
	 * @return the read parameters
	 */
	protected static Map<String, String> scanRawForParameters(final String raw) {
		return scanRawForParameters(raw, PARAMETER_SEPARATOR,
				NAME_VALUE_SEPARATOR);
	}

	/**
	 * Scans the passed {@code raw} string for parameters.
	 * 
	 * @param raw
	 *            the string to be parsed
	 * @param paramSeparator
	 *            the separator used to separate the different parameter from
	 *            each other
	 * @param nameValueSeparator
	 *            the separator used to separate the key from the value
	 * 
	 * @return the read parameters
	 */
	protected static Map<String, String> scanRawForParameters(final String raw,
			final String paramSeparator, final String nameValueSeparator) {
		final Map<String, String> parameters = new HashMap<String, String>();

		final Scanner scanner = new Scanner(raw);
		scanner.useDelimiter(paramSeparator);

		// parse the parameter
		while (scanner.hasNext()) {
			final String token = scanner.next();

			// get the position of the separator
			final int position = token.indexOf(nameValueSeparator);

			// get the name and the value
			final String name;
			final String value;
			if (position != -1) {
				name = token.substring(0, position).trim();
				value = token.substring(position + 1).trim();
			} else {
				name = token.trim();
				value = null;
			}

			// add the parameter
			parameters.put(decode(name), decode(value));
		}

		return parameters;
	}

	/**
	 * Decodes the passed <code>value</code> using the default
	 * <code>URLDecoder</code>.
	 * 
	 * @param value
	 *            the encoded string
	 * 
	 * @return the decoded string
	 * 
	 * @see URLDecoder
	 */
	public static String decode(final String value) {

		if (value == null) {
			return value;
		} else {
			try {
				return URLDecoder.decode(value, Consts.UTF_8.toString());
			} catch (final UnsupportedEncodingException e) {
				throw new IllegalStateException("Unexpected exception thrown",
						e);
			}
		}
	}
}
