package net.meisen.general.server.http.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Locale;

import net.meisen.general.genmisc.types.Files;
import net.meisen.general.server.http.listener.exceptions.HttpListenerException;
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
	 * Tests the registration of the HTTP listener using the working directory as
	 * docRoot
	 */
	@Test
	public void testWorkingDocRoot() {
		System.setProperty("server.settings.selector",
				"serverHttp-test-workingDirAsDocDir.xml");

		// get the test-subject
		final HttpListener httpListener = TestHelper.getHttpListener();

		// check the document root
		assertEquals(Files.getCanonicalPath("."), httpListener.getDocRoot());
	}

	/**
	 * Tests the throwing of an exception, i.e. if the docRoot wasn't defined
	 */
	@Test
	public void testInvalidDocDir() {
		final Locale defLocale = Locale.getDefault();
		Locale.setDefault(new Locale("en"));

		System.setProperty("server.settings.selector",
				"serverHttp-test-invalidDocDir.xml");

		// get the test-subject and catch the expected exception
		try {
			TestHelper.getHttpListener();
			fail("Exception was not thrown");
		} catch (final Exception e) {
			assertTrue(e instanceof HttpListenerException);
			assertEquals(
					"The document-root has to be specified for any http-listener",
					e.getMessage());
		}

		Locale.setDefault(defLocale);
	}
}
