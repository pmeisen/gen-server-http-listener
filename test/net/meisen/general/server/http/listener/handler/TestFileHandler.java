package net.meisen.general.server.http.listener.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.meisen.general.genmisc.types.Files;
import net.meisen.general.server.http.listener.HttpListener;
import net.meisen.general.server.http.listener.exceptions.FileHandlerException;
import net.meisen.general.server.http.listener.testutilities.TestHelper;
import net.meisen.general.server.settings.pojos.Extension;

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
	 * Tests the determination of a prefix when no <code>urlMatcher</code>, i.e.
	 * <code>null</code>, is defined.
	 */
	@Test
	public void testPrefixDeterminationWithNull() {

		// create the test-subject
		final FileHandler h = new FileHandler();
		assertEquals("", h.determinePrefix(null));
	}

	/**
	 * Tests the determination of a prefix when an empty <code>urlMatcher</code>
	 * is defined.
	 */
	@Test
	public void testPrefixDeterminationWithEmptyMatcher() {

		// create the extension
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "");

		// create the test-subject
		final FileHandler h = new FileHandler();
		assertEquals("", h.determinePrefix(e));
	}

	/**
	 * Tests the determination of a prefix when a global <code>urlMatcher</code>
	 * , i.e. <code>*</code>, is defined.
	 */
	@Test
	public void testPrefixDeterminationWithFullMatcher() {

		// create the extension
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "*");

		// create the test-subject
		final FileHandler h = new FileHandler();
		assertEquals("", h.determinePrefix(e));
	}

	/**
	 * Tests the determination of a prefix when a <code>urlMatcher</code> is
	 * defined, which can only matches files.
	 */
	@Test
	public void testPrefixDeterminationWithFileMatcher() {

		// create the extension
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "/myFile");

		// create the test-subject
		final FileHandler h = new FileHandler();
		assertEquals("/", h.determinePrefix(e));
	}

	/**
	 * Tests the determination of a prefix when an <code>urlMatcher</code> is
	 * defined, which matches anything with a just prefix (and no defined
	 * folder-structure).
	 */
	@Test
	public void testPrefixDeterminationWithFilePrefixMatcher() {

		// create the extension
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "/myFile*");

		// create the test-subject
		final FileHandler h = new FileHandler();
		assertEquals("/", h.determinePrefix(e));
	}

	/**
	 * Tests the determination of a prefix when a <code>urlMatcher</code> is
	 * defined, which defines a folder-structure defined.
	 */
	@Test
	public void testPrefixDeterminationWithGlobalFolderMatcher() {

		// create the extension
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "/myFolder/*");

		// create the test-subject
		final FileHandler h = new FileHandler();
		assertEquals("/myFolder/", h.determinePrefix(e));
	}

	/**
	 * Tests the determination of a prefix when a <code>urlMatcher</code> is
	 * defined, which defines a folder-structure followed by a prefix.
	 */
	@Test
	public void testPrefixDeterminationWithFolderAndPrefixFileMatcher() {

		// create the extension
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "/myFolder/myPrefix*");

		// create the test-subject
		final FileHandler h = new FileHandler();
		assertEquals("/myFolder/", h.determinePrefix(e));
	}

	/**
	 * Tests the determination of a prefix when a <code>urlMatcher</code> is
	 * defined, which has just a folder, i.e. no file or prefix matching
	 * afterwards. This normally makes no sense, but could have been defined.
	 */
	@Test
	public void testPrefixDeterminationWithJustFolder() {

		// create the extension
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "/myFolder/");

		// create the test-subject
		final FileHandler h = new FileHandler();
		assertEquals("/myFolder/", h.determinePrefix(e));
	}

	/**
	 * Test a folder-<code>urlMatcher</code> and an <code>URI</code> which is
	 * <code>null</code>.
	 * 
	 * @throws IOException
	 *             if the decoding fails
	 */
	@Test
	public void testFileDeterminationWithNull() throws IOException {

		// create the extension
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "*");

		// create the test-subject
		final FileHandler h = new FileHandler();
		h.initialize(e);

		// check the result
		final File file = h.determineFile(null);
		assertNull(file);
	}

	/**
	 * Test a folder-<code>urlMatcher</code> and an empty <code>URI</code>.
	 * 
	 * @throws IOException
	 *             if the decoding fails
	 */
	@Test
	public void testFileDeterminationWithEmptyUri() throws IOException {

		// create the extension
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "");

		// create the test-subject
		final FileHandler h = new FileHandler();
		h.initialize(e);

		// check the result
		final File file = h.determineFile("");
		assertNull(file);
	}

	/**
	 * Test a global-<code>urlMatcher</code> and an <code>URI</code> which
	 * contains a folder.
	 * 
	 * @throws IOException
	 *             if the decoding fails
	 */
	@Test
	public void testFileDeterminationGlobalMatcherAndFolderUri()
			throws IOException {
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "*");
		e.setProperty(FileHandler.PROPERTY_DOCROOT, testDir.getAbsolutePath());

		// create the file
		final File tmpDir = new File(testDir, "doc");
		assertTrue(tmpDir.exists() || tmpDir.mkdirs());
		assertTrue(new File(testDir, "doc/index.html").createNewFile());

		final FileHandler h = new FileHandler();
		h.initialize(e);

		// get the file
		final File file = h.determineFile("/doc/index.html");

		// check the retrieved file
		assertEquals(new File(new File(testDir, "doc"), "index.html").getCanonicalFile(), file);
		assertTrue(Files.deleteDir(tmpDir));
	}

	/**
	 * Test a folder-<code>urlMatcher</code> and an <code>URI</code> which
	 * contains the folder.
	 * 
	 * @throws IOException
	 *             if the decoding fails
	 */
	@Test
	public void testFileDeterminationFolderMatcherAndFolderUri()
			throws IOException {
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "/doc/*");
		e.setProperty(FileHandler.PROPERTY_DOCROOT, testDir.getAbsolutePath());

		// create the files
		final File tmpDir = new File(testDir, "afolder");
		final File tmpFile = new File(testDir, "index.html");
		assertTrue(tmpDir.mkdirs());
		assertTrue(tmpFile.createNewFile());
		assertTrue(new File(testDir, "afolder/index.html").createNewFile());

		final FileHandler h = new FileHandler();
		h.initialize(e);

		// the test subject
		File file;

		// check without sub-folder
		file = h.determineFile("/doc/index.html");
		assertEquals(new File(testDir, "index.html").getCanonicalFile(), file);

		// check with sub-folder
		file = h.determineFile("/doc/afolder/index.html");
		assertEquals(new File(testDir, "/afolder/index.html").getCanonicalFile(), file);
		assertTrue(tmpFile.delete());
		assertTrue(Files.deleteDir(tmpDir));
	}

	/**
	 * Test an invalid match, i.e. the <code>FileHandler</code> should never be
	 * called for that match.
	 * 
	 * @throws IOException
	 *             if the decoding fails
	 */
	@Test
	public void testFileDeterminationFilePrefixMatcherAndFolderUri()
			throws IOException {
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "/doc/sub/file*");
		e.setProperty(FileHandler.PROPERTY_DOCROOT, testDir.getAbsolutePath());

		// create the files
		final File tmpDir = new File(testDir, "another");
		final File tmpFile = new File(testDir, "fileSample.html");
		assertTrue(tmpDir.mkdirs());
		assertTrue(tmpFile.createNewFile());
		assertTrue(new File(testDir, "another/fileSample.html").createNewFile());

		final FileHandler h = new FileHandler();
		h.initialize(e);

		// the test-subject
		File file;

		// check without sub-folder
		file = h.determineFile("/doc/sub/fileSample.html");
		assertEquals(new File(testDir, "fileSample.html").getCanonicalFile(), file);

		// check with sub-folder
		file = h.determineFile("/doc/sub/another/fileSample.html");
		assertEquals(new File(testDir, "another/fileSample.html").getCanonicalFile(), file);

		assertTrue(tmpFile.delete());
		assertTrue(Files.deleteDir(tmpDir));
	}

	/**
	 * Tests the usage of multiple locations
	 * 
	 * @throws IOException
	 *             if a test-file cannot be created or if the decoding failed
	 */
	@Test
	public void testFileWithMultipleLocations() throws IOException {
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "/doc/*");
		e.setProperty(FileHandler.PROPERTY_DOCROOT, testDir.getAbsolutePath());

		// get a default name
		final String defName = FileHandler.DEF_DEFFILES.split(",")[0].trim();

		// place one file in the main-location
		final File tmpFile = new File(testDir, "aMainFile.file");
		assertTrue(tmpFile.createNewFile());

		// create the files
		final List<Extension> extensions = new ArrayList<Extension>();
		final File tmpDir = new File(testDir, "doc");
		assertTrue(tmpDir.mkdirs());
		for (int i = 1; i < 10; i++) {
			final File dir = new File(tmpDir, "" + i);
			assertTrue(dir.mkdirs());

			// create the same file in every folder
			assertTrue(new File(dir, "normalized.file").createNewFile());

			// create a specific file in every folder
			assertTrue(new File(dir, "normalized_" + i + ".file")
					.createNewFile());

			// create a file in every 2 folder
			if (i % 2 == 0) {
				assertTrue(new File(dir, "normalized_even.file")
						.createNewFile());
			}

			if (i % 9 == 0) {
				assertTrue(new File(dir, defName).createNewFile());
				assertTrue(new File(dir, "aMainFile.file").createNewFile());
			}

			// add as location
			final Extension ex = new Extension();
			ex.setId(FileHandler.EXTENSION_LOCATION);
			ex.setProperty(FileHandler.PROPERTY_DOCROOT, dir.getAbsolutePath());
			extensions.add(ex);
		}
		e.setExtensions(extensions);

		final FileHandler h = new FileHandler();
		h.initialize(e);

		// the test-subject
		File file;

		// check simple retrieval
		file = h.determineFile("/doc/normalized.file");
		assertEquals(new File(tmpDir, "1/normalized.file").getCanonicalFile(), file);

		// check specific retrieval of a file which is only in one location
		file = h.determineFile("/doc/normalized_3.file");
		assertEquals(new File(tmpDir, "3/normalized_3.file").getCanonicalFile(), file);

		// check specific retrieval of a file which is available multiple times
		file = h.determineFile("/doc/normalized_even.file");
		assertEquals(new File(tmpDir, "2/normalized_even.file").getCanonicalFile(), file);

		// check a default name
		file = h.determineFile("/doc/");
		assertEquals(new File(tmpDir, "9/" + defName).getCanonicalFile(), file);

		// check the main-folder
		file = h.determineFile("/doc/aMainFile.file");
		assertEquals(new File(testDir, "aMainFile.file").getCanonicalFile(), file);

		// cleanUp
		assertTrue(tmpFile.delete());
		assertTrue(Files.deleteDir(tmpDir));
	}

	/**
	 * Test an invalid match, i.e. the <code>FileHandler</code> should never be
	 * called for that match.
	 * 
	 * @throws IOException
	 *             if the decoding fails
	 */
	@Test
	public void testFileDeterminationWithInvalidMatch() throws IOException {
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "/file/match*");
		e.setProperty(FileHandler.PROPERTY_DOCROOT, testDir.getAbsolutePath());

		final FileHandler h = new FileHandler() {
			@Override
			protected void validateDocRoot(final List<String> docRoot)
					throws FileHandlerException {
				// disable the validation
				return;
			}
		};
		h.initialize(e);

		// the test-subject
		File file;

		// check without sub-folder
		file = h.determineFile("/doc/sub/fileSample.html");
		assertNull(file);
	}

	/**
	 * Tests the match to a file instead of a directory
	 * 
	 * @throws IOException
	 *             if the test-file cannot be created or decoding fails
	 */
	@Test
	public void testFileDeterminationForFile() throws IOException {
		final Extension e = new Extension();
		e.setProperty(HttpListener.PROPERTY_URLMATCHER, "/file/match*");
		final File rndFile = new File(testDir, UUID.randomUUID().toString());
		rndFile.createNewFile();
		e.setProperty(FileHandler.PROPERTY_DOCROOT, rndFile.getAbsolutePath());

		final FileHandler h = new FileHandler() {
			@Override
			protected void validateDocRoot(final List<String> docRoot)
					throws FileHandlerException {
				// disable the validation
				return;
			}
		};
		h.initialize(e);

		// the test-subject
		File file;

		// check without sub-folder
		file = h.determineFile("/file/matchwhatsoever");
		assertEquals(rndFile.getCanonicalFile(), file);
	}

	/**
	 * Tests the determination of default files with a <code>null</code>
	 * <code>Extension</code>.
	 */
	@Test
	public void testDefFilesDeterminationOfNull() {
		final Extension e = new Extension();
		e.setProperty(FileHandler.PROPERTY_DEFFILES, null);

		final FileHandler h = new FileHandler();
		final List<String> l = h.determineDefaultFiles(e);
		assertArrayEquals(FileHandler.DEF_DEFFILES.split("\\s*,\\s*"),
				l.toArray());
	}

	/**
	 * Tests the determination of default files with an empty
	 * <code>Extension</code>.
	 */
	@Test
	public void testDefFilesDeterminationOfEmpty() {
		final Extension e = new Extension();
		e.setProperty(FileHandler.PROPERTY_DEFFILES, "    ");

		final FileHandler h = new FileHandler();
		final List<String> l = h.determineDefaultFiles(e);
		assertArrayEquals(new String[] {}, l.toArray());
	}

	/**
	 * Tests the determination of default files with one file.
	 */
	@Test
	public void testDefFilesDeterminationOfDefaultFilesOneFile() {
		final Extension e = new Extension();
		e.setProperty(FileHandler.PROPERTY_DEFFILES, "myDefFiles.asp");

		final FileHandler h = new FileHandler();
		final List<String> l = h.determineDefaultFiles(e);
		assertNotNull(l);
		assertEquals(1, l.size());
		assertEquals("myDefFiles.asp", l.get(0));
	}

	/**
	 * Tests the determination of default files with several files.
	 */
	@Test
	public void testDefFilesDeterminationOfDefaultFilesSeveralFiles() {
		final Extension e = new Extension();
		e.setProperty(FileHandler.PROPERTY_DEFFILES,
				"myDefFiles.asp  , another.html,  index.html");

		final FileHandler h = new FileHandler();
		final List<String> l = h.determineDefaultFiles(e);
		assertNotNull(l);
		assertEquals(3, l.size());
		assertEquals("myDefFiles.asp", l.get(0));
		assertEquals("another.html", l.get(1));
		assertEquals("index.html", l.get(2));
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
		Files.writeToFile(file, expFileContent, "UTF-8");

		// get the response
		final byte[] response = TestHelper.getResponse(httpListener.getPort(),
				file.getName());
		final String fileContent = new String(response, "UTF-8");

		// check the result
		assertEquals(expFileContent, fileContent);
	}

	/**
	 * Tests the retrieval of a file via the handler specifying parameters with
	 * it.
	 * 
	 * @throws ClientProtocolException
	 *             if the client cannot access the server with the specified
	 *             protocol (should never happen)
	 * @throws IOException
	 *             if the file or data cannot be read
	 */
	@Test
	public void testFileRetrievalWithParameters()
			throws ClientProtocolException, IOException {
		final String expFileContent = "This is a test-entry";

		// create a test-file which we want to retrieve
		final File file = new File(testDir, UUID.randomUUID().toString());
		Files.writeToFile(file, expFileContent, "UTF-8");

		// get the response
		final byte[] response = TestHelper.getResponse(httpListener.getPort(),
				file.getName() + "?language=test");
		final String fileContent = new String(response, "UTF-8");

		// check the result
		assertEquals(expFileContent, fileContent);
	}

	/**
	 * Check the retrieval of a default-file.
	 * 
	 * @throws ClientProtocolException
	 *             if the client cannot access the server with the specified
	 *             protocol (should never happen)
	 * @throws IOException
	 *             if the file or data cannot be read
	 */
	@Test
	public void testDefaultFileRetrieval() throws ClientProtocolException,
			IOException {
		final String expFileContent = "This is a default file";

		// create a test-file which we want to retrieve
		final File dir = new File(testDir, UUID.randomUUID().toString());
		assertTrue(dir.mkdirs());
		final File defFile = new File(dir, "myDefault.html");
		Files.writeToFile(defFile, expFileContent, "UTF-8");

		// get the response
		final byte[] response = TestHelper.getResponse(httpListener.getPort(),
				defFile.getParentFile().getName());
		final String fileContent = new String(response, "UTF-8");

		// check the result
		assertEquals(expFileContent, fileContent);
	}

	/**
	 * Check the retrieval of a default-file, while specifying parameters.
	 * 
	 * @throws ClientProtocolException
	 *             if the client cannot access the server with the specified
	 *             protocol (should never happen)
	 * @throws IOException
	 *             if the file or data cannot be read
	 */
	@Test
	public void testDefaultFileRetrievalWithParameters()
			throws ClientProtocolException, IOException {
		final String expFileContent = "This is a default file";

		// create a test-file which we want to retrieve
		final File dir = new File(testDir, UUID.randomUUID().toString());
		assertTrue(dir.mkdirs());
		final File defFile = new File(dir, "myDefault.html");
		Files.writeToFile(defFile, expFileContent, "UTF-8");

		// get the response
		final byte[] response = TestHelper.getResponse(httpListener.getPort(),
				defFile.getParentFile().getName() + "?language=test");
		final String fileContent = new String(response, "UTF-8");

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
