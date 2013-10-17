package net.meisen.general.server.http.listener.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.Consts;
import org.apache.http.HttpRequest;

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
	 * Get the parameters of a <code>HttpRequest</code>.
	 * 
	 * @param request
	 *            the <code>HttpRequest</code> to parse for parameters
	 * 
	 * @return the found parameters, can never be <code>null</code>
	 */
	public static Map<String, String> parseParameter(final HttpRequest request) {
		final Map<String, String> parameters = new HashMap<String, String>();

		// check the request
		if (request == null) {
			return parameters;
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
			return parameters;
		}

		// work with the retrieved URI
		final Scanner scanner = new Scanner(rawQuery);
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
