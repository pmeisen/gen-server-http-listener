package net.meisen.general.server.http.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.meisen.general.genmisc.types.Files;
import net.meisen.general.server.http.listener.testutilities.TestHelper;

import org.junit.Test;

public class TestHttpListenerControl {

	@Test
	public void testClosing() {
		System.setProperty("server.settings.selector",
				"serverHttp-test-workingDirAsDocDir.xml");

		// get the test-subject
		final HttpListener httpListener = TestHelper.getHttpListener();

		// check the document root
		assertEquals(Files.getCanonicalPath("."), httpListener.getDocRoot());

		// start and shut the listener again
		httpListener.open();
		assertFalse(httpListener.isClosed());
		httpListener.close();

		while (!httpListener.isClosed()) {
			try {
				Thread.sleep(10);
			} catch (final InterruptedException e) {
				// nothing to do
			}
		}
		assertTrue(httpListener.isClosed());
	}
	
	@Test
	public void testClosingAfterSleep() throws InterruptedException {
		System.setProperty("server.settings.selector",
				"serverHttp-test-workingDirAsDocDir.xml");

		// get the test-subject
		final HttpListener httpListener = TestHelper.getHttpListener();

		// check the document root
		assertEquals(Files.getCanonicalPath("."), httpListener.getDocRoot());

		// start and shut the listener again
		httpListener.open();
		assertFalse(httpListener.isClosed());
		
		// sleep a little and close it
		Thread.sleep(1000);
		httpListener.close();

		while (!httpListener.isClosed()) {
			try {
				Thread.sleep(10);
			} catch (final InterruptedException e) {
				// nothing to do
			}
		}
		assertTrue(httpListener.isClosed());
	}
}
