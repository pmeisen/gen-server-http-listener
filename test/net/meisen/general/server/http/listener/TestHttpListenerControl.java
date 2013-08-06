package net.meisen.general.server.http.listener;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.meisen.general.server.http.listener.testutilities.TestHelper;

import org.junit.Test;

/**
 * Tests the implementation of the <code>HttpListener</code>.
 * 
 * @author pmeisen
 * 
 */
public class TestHttpListenerControl {

	/**
	 * Tests the closing of a <code>HttpListener</code>.
	 */
	@Test
	public void testClosing() {
		System.setProperty("server.settings.selector",
				"serverHttp-test-workingDirAsDocDir.xml");

		// get the test-subject
		final HttpListener httpListener = TestHelper.getHttpListener();

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

	/**
	 * Tests the closing after some time of sleeping.
	 * 
	 * @throws InterruptedException
	 *             if the sleep is interrupted
	 */
	@Test
	public void testClosingAfterSleep() throws InterruptedException {
		System.setProperty("server.settings.selector",
				"serverHttp-test-workingDirAsDocDir.xml");

		// get the test-subject
		final HttpListener httpListener = TestHelper.getHttpListener();

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
