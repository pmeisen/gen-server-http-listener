package net.meisen.general.server.http.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import net.meisen.general.server.http.listener.api.IHandler;
import net.meisen.general.server.http.listener.handler.FileHandler;
import net.meisen.general.server.http.listener.handler.ServletHandler;
import net.meisen.general.server.http.listener.testutilities.TestHelper;

import org.junit.Test;

/**
 * Tests the implementation of a <code>HttpListener</code>
 * 
 * @author pmeisen
 * 
 */
public class TestHttpListenerRegistration {

	/**
	 * Tests the registration of the HTTP listener using the working directory
	 * as docRoot
	 */
	@Test
	public void testWorkingDocRoot() {
		System.setProperty("server.settings.selector",
				"serverHttp-test-workingDirAsDocDir.xml");

		// get the test-subject
		final HttpListener httpListener = TestHelper.getHttpListener();

		// check the document root
		assertEquals(1, httpListener.getHandlers().size());

		// get the handler
		final IHandler handler = httpListener.getHandlers().values().iterator()
				.next();
		assertTrue(handler instanceof FileHandler);
	}

	/**
	 * Tests the definition of several <code>Handler</code> instances.
	 */
	@Test
	public void testSeveralHandlers() {
		System.setProperty("server.settings.selector",
				"serverHttp-test-DocDirAndServlet.xml");

		// get the test-subject
		final HttpListener httpListener = TestHelper.getHttpListener();
		assertEquals(2, httpListener.getHandlers().size());

		final IHandler fileHandler = httpListener.getHandlers().get("*");
		assertTrue(fileHandler instanceof FileHandler);

		final IHandler servletHandler = httpListener.getHandlers().get(
				"servlet/*");
		assertTrue(servletHandler instanceof ServletHandler);
	}
}
