package net.meisen.general.server.http.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import net.meisen.general.server.http.listener.api.IHandler;
import net.meisen.general.server.http.listener.api.IServlet;
import net.meisen.general.server.http.listener.exceptions.ServletHandlerException;
import net.meisen.general.server.http.listener.handler.ServletHandler;
import net.meisen.general.server.http.listener.testutilities.TestHelper;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

/**
 * Tests the <code>ServletHandler</code> implementation.
 * 
 * @author pmeisen
 * 
 */
public class TestServletHandler {

	/**
	 * An implementation of a <code>Servlet</code> for testing purposes only.
	 * 
	 * @author pmeisen
	 * 
	 */
	public static class TestServlet implements IServlet {

		@Override
		public void handle(final HttpRequest request,
				final HttpResponse response, final HttpContext context) {
			response.setStatusCode(HttpStatus.SC_OK);
			response.setEntity(new StringEntity("TESTSERVLET",
					ContentType.DEFAULT_TEXT));
		}
	}

	/**
	 * Helper method to get the defined <code>ServletHandler</code>.
	 * 
	 * @param fileName
	 *            the file use to load the handler definition
	 * @return the defined <code>ServletHandler</code>
	 */
	protected ServletHandler getHandler(final String fileName) {
		System.setProperty("server.settings.selector", fileName);

		// get the test-subject
		final HttpListener httpListener = TestHelper.getHttpListener();

		// check the document root
		assertEquals(1, httpListener.getHandlers().size());

		// get the handler
		final IHandler handler = httpListener.getHandlers().values().iterator()
				.next();
		assertTrue(handler instanceof ServletHandler);

		return (ServletHandler) handler;
	}

	/**
	 * Tests if <code>ServletHandler</code> throws an exception when an
	 * undefined class is used.
	 */
	@Test
	public void testNotDefined() {
		final Locale def = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);

		try {
			getHandler("serverHttp-test-unavailableServletClass.xml");
		} catch (final Exception e) {
			assertTrue(e instanceof ServletHandlerException);
			assertEquals(
					"The servlet-class 'net.meisen.not.available.never.ever.hopefully' cannot be found on the classpath.",
					e.getMessage());
		} finally {
			Locale.setDefault(def);
		}
	}

	/**
	 * Tests if <code>ServletHandler</code> throws an exception when a class is
	 * used, which is not a sub-class of a <code>IServlet</code>.
	 */
	@Test
	public void testValidClassButNotIServlet() {
		final Locale def = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);

		try {
			getHandler("serverHttp-test-invalidServletClass.xml");
		} catch (final Exception e) {
			assertTrue(e instanceof ServletHandlerException);
			assertEquals("The servlet-class '" + AllTests.class.getName()
					+ "' has to implement the IServlet interface.",
					e.getMessage());
		} finally {
			Locale.setDefault(def);
		}
	}

	/**
	 * Tests the usage of a <code>Servlet</code>.
	 * 
	 * @throws UnsupportedEncodingException
	 *             if the encoding is not supported
	 */
	@Test
	public void testUsage() throws UnsupportedEncodingException {
		System.setProperty("server.settings.selector",
				"serverHttp-test-testServlet.xml");

		// get the test-subject
		final HttpListener httpListener = TestHelper.getHttpListener();

		// start the listener
		httpListener.open();

		final byte[] response = TestHelper.getResponse(httpListener.getPort(),
				"");
		final String content = new String(response, "UTF8");
		assertEquals("TESTSERVLET", content);

		// close the listener
		httpListener.close();
	}
}
