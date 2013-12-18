package net.meisen.general.server.http.listener.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.util.EntityUtils;

/**
 * Utility class when working with <code>HttpRequest</code> instances.
 * 
 * @author pmeisen
 * 
 */
public class RequestHandlingUtilities {
	private static final String PARAMETER_SEPARATOR = "&";
	private static final String NAME_VALUE_SEPARATOR = "=";

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
	 * Scans the passed {@code raw} string for parameters.
	 * 
	 * @param raw
	 *            the string to be parsed
	 * 
	 * @return the read parameters
	 */
	protected static Map<String, String> scanRawForParameters(final String raw) {
		final Map<String, String> parameters = new HashMap<String, String>();

		final Scanner scanner = new Scanner(raw);
		scanner.useDelimiter(PARAMETER_SEPARATOR);

		// parse the parameter
		while (scanner.hasNext()) {
			final String token = scanner.next();

			// get the position of the separator
			final int position = token.indexOf(NAME_VALUE_SEPARATOR);

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
