package net.meisen.general.server.http.listener.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.meisen.general.genmisc.types.Files;
import net.meisen.general.genmisc.types.Streams;
import net.meisen.general.sbconfigurator.runners.JUnitConfigurationRunner;
import net.meisen.general.sbconfigurator.runners.annotations.ContextClass;
import net.meisen.general.sbconfigurator.runners.annotations.ContextFile;
import net.meisen.general.sbconfigurator.runners.annotations.SystemProperty;
import net.meisen.general.server.Server;
import net.meisen.general.server.http.listener.api.IServlet;
import net.meisen.general.server.http.listener.testutilities.TestHelper;
import net.meisen.general.server.settings.pojos.Extension;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Tests the implementation of the <code>RequestHandlingUtilities</code>.
 * 
 * @author pmeisen
 * 
 */
@RunWith(JUnitConfigurationRunner.class)
@ContextClass(Server.class)
@ContextFile("sbconfigurator-core-useSystemProperties.xml")
@SystemProperty(property = "server.settings.selector", value = "serverHttp-test-requestFileHandling.xml")
public class TestRequestFileHandlingUtilities {

	/**
	 * Helper servlet to test the file upload.
	 * 
	 * @author pmeisen
	 * 
	 */
	public static class TestServletFileUpload implements IServlet {
		private static final List<FileItem> fileItems = new ArrayList<FileItem>();

		@Override
		public void initialize(final Extension e) {
			// nothing to do
		}

		@Override
		public void handle(final HttpRequest request,
				final HttpResponse response, final HttpContext context) {

			try {
				final List<FileItem> files = RequestFileHandlingUtilities
						.handleFileUpload(request);
				final HashMap<String, String> fileMap = new HashMap<String, String>();
				for (final FileItem fileItem : files) {
					fileMap.put(fileItem.getName(), Files
							.getCanonicalPath(((DiskFileItem) fileItem)
									.getStoreLocation()));
				}

				response.setEntity(new SerializableEntity(fileMap, false));
				fileItems.addAll(files);
			} catch (Exception e) {
				response.setEntity(new StringEntity("ERROR",
						ContentType.DEFAULT_TEXT));
			}
		}

		/**
		 * Deletes all the retrieved uploaded files
		 */
		public static synchronized void cleanUp() {
			for (final FileItem fileItem : fileItems) {
				try {
					Streams.closeIO(fileItem.getOutputStream());
				} catch (final IOException e) {
					// nothing to do
				}

				fileItem.delete();

			}
		}
	}

	@Autowired
	@Qualifier("server")
	private Server server;

	/**
	 * Starts the server
	 * 
	 * @throws InterruptedException
	 *             if the connection failed
	 */
	@Before
	public void before() throws InterruptedException {
		// now start the server
		server.startAsync();

		// make sure the server started
		Thread.sleep(100);
	}

	/**
	 * Tests the implementation of
	 * {@code RequestHandlingUtilities#handleFileUpload(HttpRequest)}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testFileUpload() throws IOException {
		final InputStream is = getClass()
				.getResourceAsStream(
						"/net/meisen/general/server/http/listener/serverHttp-test-DocDirAndServlet.xml");
		assertNotNull(is);

		final MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder
				.create();
		multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		multipartEntityBuilder.setCharset(Consts.UTF_8);
		multipartEntityBuilder.addBinaryBody("SampleUpload", is,
				ContentType.create("text/xml", Consts.UTF_8), "sample.upload");

		// create the client
		final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		final Builder requestConfigBuilder = RequestConfig.custom();
		requestConfigBuilder.setExpectContinueEnabled(false);
		httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
		final CloseableHttpClient httpClient = httpClientBuilder.build();

		// create the post and host
		final HttpHost httpHost = new HttpHost("localhost", 6060);
		final HttpPost httpPost = new HttpPost("/fileUpload");
		httpPost.setEntity(multipartEntityBuilder.build());

		// execute it
		Object answer = null;
		try {
			final HttpResponse response = httpClient
					.execute(httpHost, httpPost);
			answer = TestHelper.getDeserialized(TestHelper
					.getResponse(response));
			Streams.closeIO(is);
			httpClient.close();
		} catch (final RuntimeException e) {
			// In case of an unexpected exception you may want to abort
			// the HTTP request in order to shut down the underlying
			// connection immediately.
			httpPost.abort();
			fail(e.getMessage());
		} catch (final Exception e) {
			fail(e.getMessage());
		}

		// check the retrieved answer
		assertNotNull(answer);
		assertTrue(answer instanceof Map);

		@SuppressWarnings("unchecked")
		final Map<String, String> mappedFiles = (Map<String, String>) answer;
		assertEquals(1, mappedFiles.size());

		for (final Entry<String, String> e : mappedFiles.entrySet()) {
			final File file = new File(e.getValue());

			assertTrue(e.getValue() + " does not exist", file.exists());
			assertEquals("sample.upload", e.getKey());
		}

		// cleanUp
		TestServletFileUpload.cleanUp();
	}

	/**
	 * Shut the server down.
	 * 
	 * @throws InterruptedException
	 *             if the waiting failed
	 */
	@After
	public void after() throws InterruptedException {
		// shut the server down again
		server.shutdown();
		Thread.sleep(100);
	}
}
