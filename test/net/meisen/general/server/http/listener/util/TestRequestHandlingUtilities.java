package net.meisen.general.server.http.listener.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import net.meisen.general.genmisc.types.Streams;
import net.meisen.general.sbconfigurator.runners.JUnitConfigurationRunner;
import net.meisen.general.sbconfigurator.runners.annotations.ContextClass;
import net.meisen.general.sbconfigurator.runners.annotations.ContextFile;
import net.meisen.general.sbconfigurator.runners.annotations.SystemProperty;
import net.meisen.general.server.Server;
import net.meisen.general.server.http.listener.api.IServlet;
import net.meisen.general.server.http.listener.testutilities.TestHelper;
import net.meisen.general.server.settings.pojos.Extension;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.entity.StringEntity;
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
@SystemProperty(property = "server.settings.selector", value = "serverHttp-test-requestHandling.xml")
public class TestRequestHandlingUtilities {

	/**
	 * Helper servlet for testing
	 * 
	 * @author pmeisen
	 * 
	 */
	public static class TestServlet implements IServlet {

		@Override
		public void initialize(final Extension e) {
			// nothing to do
		}

		@Override
		public void handle(final HttpRequest request,
				final HttpResponse response, final HttpContext context) {
			response.setStatusCode(HttpStatus.SC_OK);

			final Map<String, String> parameters = RequestHandlingUtilities
					.parseParameter(request);

			try {
				response.setEntity(new SerializableEntity(
						(HashMap<String, String>) parameters, false));
			} catch (final Exception e) {
				response.setEntity(new StringEntity("ERROR",
						ContentType.DEFAULT_TEXT));
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

	@SuppressWarnings("unchecked")
	private HashMap<String, String> fire(final String suffix) {
		final Object object = TestHelper.getDeserializedResponse(6060, suffix);
		assertTrue(object instanceof HashMap);

		return (HashMap<String, String>) object;
	}

	/**
	 * Tests the retrieval of no parameters
	 */
	@Test
	public void testEmptyParameter() {

		// get the map
		final HashMap<String, String> map = fire("");
		assertEquals(0, map.size());
	}

	/**
	 * Tests the retrieval of exactly one parameter
	 * 
	 * @throws UnsupportedEncodingException
	 *             if the decoding failed
	 */
	@Test
	public void testSingleParameter() throws UnsupportedEncodingException {
		final String url = "/test?alles="
				+ URLEncoder.encode("es ist super mit = äüö", "UTF8");

		// get the map
		final HashMap<String, String> map = fire(url);
		assertEquals(1, map.size());
		assertEquals("es ist super mit = äüö", map.get("alles"));
	}

	/**
	 * Tests the retrieval of exactly one parameter
	 */
	@Test
	public void testMultipleParameter() {
		final String url = "/test?one=isOne&two=isTwo";

		// get the map
		final HashMap<String, String> map = fire(url);
		assertEquals(2, map.size());
		assertEquals("isOne", map.get("one"));
		assertEquals("isTwo", map.get("two"));
	}

	/**
	 * Tests the retrieval of post parameters.
	 * 
	 * @throws IOException
	 *             if the connection could not be open, read or written
	 */
	@Test
	public void testPostValues() throws IOException {

		// get the connection setup
		final URL url = new URL("http://localhost:6060/test?lala=lulu");
		final HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();
		connection.setDoOutput(true);

		// define the parameters to be send via post
		final String urlParameters = "param1=a&param2=b&param3="
				+ URLEncoder.encode("es ist super mit = äüö", "UTF8");
		;

		// write the post stuff to the connection
		final OutputStreamWriter writer = new OutputStreamWriter(
				connection.getOutputStream());
		writer.write(urlParameters);
		writer.flush();

		final Object o = TestHelper.getDeserialized(Streams
				.copyStreamToByteArray(connection.getInputStream()));
		assertTrue(o instanceof HashMap);

		@SuppressWarnings("unchecked")
		final HashMap<String, String> params = (HashMap<String, String>) o;
		assertEquals(params.get("param1"), "a");
		assertEquals(params.get("param2"), "b");
		assertEquals(params.get("param3"), "es ist super mit = äüö");

		// close connection
		connection.disconnect();
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
