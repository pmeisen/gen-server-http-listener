package net.meisen.general.server.http.listener.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
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
	public static class TestServletSerializeRequestParameters implements
			IServlet {

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

	/**
	 * Servlet reading the cookie-values.
	 * 
	 * @author pmeisen
	 * 
	 */
	public static class TestServletSerializeCookies implements IServlet {

		@Override
		public void initialize(final Extension e) {
			// nothing to do
		}

		@Override
		public void handle(final HttpRequest request,
				final HttpResponse response, final HttpContext context) {
			response.setStatusCode(HttpStatus.SC_OK);

			final Map<String, String> parameters = RequestHandlingUtilities
					.parseCookies(request);

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

	private Object fire(final String suffix) {
		final Object object = TestHelper.getDeserializedResponse(6060, suffix);
		assertTrue(object instanceof HashMap);

		return object;
	}

	/**
	 * Tests the retrieval of no parameters
	 */
	@Test
	public void testEmptyParameter() {

		// get the map
		@SuppressWarnings("unchecked")
		final HashMap<String, String> map = (HashMap<String, String>) fire("");
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
		@SuppressWarnings("unchecked")
		final HashMap<String, String> map = (HashMap<String, String>) fire(url);
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
		@SuppressWarnings("unchecked")
		final HashMap<String, String> map = (HashMap<String, String>) fire(url);
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
	 * Tests the retrieval of cookie values.
	 */
	@Test
	public void testCookies() {

		// create the client
		final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		final Builder requestConfigBuilder = RequestConfig.custom();
		final BasicCookieStore cookieStore = new BasicCookieStore();
		requestConfigBuilder.setExpectContinueEnabled(false);
		httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
		httpClientBuilder.setDefaultCookieStore(cookieStore);

		// add a cookie
		BasicClientCookie cookie;
		cookie = new BasicClientCookie("myCookie1",
				"myValue1 contains äüö \\ \" values");
		cookie.setDomain("localhost");
		cookieStore.addCookie(cookie);
		cookie = new BasicClientCookie("myCookie2", "myValue2");
		cookie.setDomain("localhost");
		cookieStore.addCookie(cookie);
		cookie = new BasicClientCookie("myCookie3", null);
		cookie.setDomain("localhost");
		cookieStore.addCookie(cookie);
		cookie = new BasicClientCookie("myCookie4", "");
		cookie.setDomain("localhost");
		cookieStore.addCookie(cookie);

		final CloseableHttpClient httpClient = httpClientBuilder.build();

		// create the post and host
		final HttpHost httpHost = new HttpHost("localhost", 6060);
		final HttpGet httpGet = new HttpGet("/cookies");

		// execute it
		Object answer = null;
		try {
			final HttpResponse response = httpClient.execute(httpHost, httpGet);
			answer = TestHelper.getDeserialized(TestHelper
					.getResponse(response));
			httpClient.close();
		} catch (final RuntimeException e) {
			// In case of an unexpected exception you may want to abort
			// the HTTP request in order to shut down the underlying
			// connection immediately.
			httpGet.abort();
			fail(e.getMessage());
		} catch (final Exception e) {
			fail(e.getMessage());
		}

		// check the retrieved answer
		assertNotNull(answer);
		assertTrue(answer instanceof Map);

		@SuppressWarnings("unchecked")
		final Map<String, String> cookies = (Map<String, String>) answer;
		assertEquals(4, cookies.size());

		assertEquals(cookies.get("myCookie1"),
				"myValue1 contains äüö \\ \" values");
		assertEquals(cookies.get("myCookie2"), "myValue2");
		assertEquals(cookies.get("myCookie3"), "");
		assertEquals(cookies.get("myCookie4"), "");

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
