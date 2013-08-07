package net.meisen.general.server.http.listener.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import net.meisen.general.genmisc.types.Files;
import net.meisen.general.server.http.listener.HttpListener;
import net.meisen.general.server.http.listener.testutilities.TestHelper;

import org.apache.http.client.ClientProtocolException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the implementation of the <code>FileHandler</code>.
 * 
 * @author pmeisen
 * 
 */
public class TestFileHandler {

	private static HttpListener httpListener = null;
	private static File testDir = null;

	/**
	 * Create a temporary directory for the test
	 */
	@BeforeClass
	public static void beforeClass() {

		// create a temporary directory
		final String tmpDirName = System.getProperty("java.io.tmpdir");
		final File tmpDir = new File(tmpDirName);
		testDir = new File(tmpDir, "test-http-listener");
		testDir.mkdir();

		// start the http listener
		System.setProperty("server.settings.selector",
				"serverHttp-test-propertyDocDir.xml");
		System.setProperty("server.settings.http.docroot",
				Files.getCanonicalPath(testDir));
		httpListener = TestHelper.getHttpListener();

		// start the listener
		httpListener.open();
	}

	/**
	 * Tests the retrieval of a file via the handler.
	 * 
	 * @throws ClientProtocolException
	 *             if the client cannot access the server with the specified
	 *             protocol (should never happen)
	 * @throws IOException
	 *             if the file or data cannot be read
	 */
	@Test
	public void testFileRetrieval() throws ClientProtocolException, IOException {
		final String expFileContent = "This is a test-entry";

		// create a test-file which we want to retrieve
		final File file = new File(testDir, UUID.randomUUID().toString());
		Files.writeToFile(file, expFileContent, "UTF8");

		// get the response
		final byte[] response = TestHelper.getResponse(httpListener.getPort(),
				file.getName());
		final String fileContent = new String(response, "UTF8");

		// check the result
		assertEquals(expFileContent, fileContent);
	}

	/**
	 * Cleans up behind the test.
	 */
	@AfterClass
	public static void afterClass() {
		// close the connection
		httpListener.close();

		// delete the directory
		assertTrue(Files.deleteDir(testDir));
	}
}
