package net.meisen.general.server.http.listener.testutilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import net.meisen.general.genmisc.types.Streams;
import net.meisen.general.sbconfigurator.ConfigurationCoreSettings;
import net.meisen.general.sbconfigurator.api.IConfiguration;
import net.meisen.general.server.Server;
import net.meisen.general.server.api.IServerSettings;
import net.meisen.general.server.http.listener.HttpListener;
import net.meisen.general.server.settings.pojos.Connector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Utilities for the tests.
 * 
 * @author pmeisen
 * 
 */
public class TestHelper {

	/**
	 * Gets the defined <code>HttpListener</code> to be used for testing
	 * 
	 * @return the defined <code>HttpListener</code>
	 */
	public static HttpListener getHttpListener() {

		// load the configuration
		final ConfigurationCoreSettings settings = ConfigurationCoreSettings
				.loadCoreSettings(
						"sbconfigurator-core-useSystemProperties.xml",
						Server.class);
		final IConfiguration config = settings.getConfiguration();

		// get the server and the settings
		final Server server = config.getModule("server");
		final IServerSettings serverSettings = server.getServerSettings();
		final Collection<Connector> connectorSettings = serverSettings
				.getConnectorSettings();

		// check if there is one
		assertEquals(1, connectorSettings.size());

		// get this first one and validate it
		final Connector c = connectorSettings.iterator().next();
		assertEquals(HttpListener.NAME, c.getListener());

		// now let's create a listener
		final HttpListener listener = config.createInstance(HttpListener.class);
		listener.initialize(c);

		// return it
		assertNotNull(listener);
		return listener;
	}

	/**
	 * Helper method to get the response from a specific <code>port</code>
	 * requesting the specified <code>suffix</code>.
	 * 
	 * @param port
	 *            the port to retrieve data from
	 * @param suffix
	 *            the suffix of the base-url to request
	 * 
	 * @return the answer as string
	 */
	public static String getStringResponse(final int port, final String suffix) {
		try {
			return new String(getResponse(port, suffix), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException();
		}
	}

	/**
	 * Helper method to get the response from a specific <code>port</code>
	 * requesting the specified <code>suffix</code>.
	 * 
	 * @param port
	 *            the port to retrieve data from
	 * @param suffix
	 *            the suffix of the base-url to request
	 * 
	 * @return the answer
	 */
	public static byte[] getResponse(final int port, final String suffix) {
		final HttpClient httpclient = new DefaultHttpClient();
		final HttpGet httpget = new HttpGet("http://localhost:" + port + "/"
				+ suffix);

		HttpEntity entity = null;
		InputStream instream = null;
		try {
			final HttpResponse response = httpclient.execute(httpget);
			entity = response.getEntity();

			// the only right way out of here
			if (entity != null) {
				instream = entity.getContent();
				return Streams.copyStreamToByteArray(instream);
			}

		} catch (final RuntimeException e) {
			// In case of an unexpected exception you may want to abort
			// the HTTP request in order to shut down the underlying
			// connection immediately.
			httpget.abort();
			fail(e.getMessage());
		} catch (final Exception e) {
			fail(e.getMessage());
		} finally {
			Streams.closeIO(instream);
			if (entity != null) {
				try {
					EntityUtils.consume(entity);
				} catch (final IOException e) {
					// ignore
				}
			}
		}

		return null;
	}

	/**
	 * Gets the request and interprets it as a serialized object using
	 * <code>ObjectInputStream</code> to deserialize.
	 * 
	 * @param port
	 *            the port to retrieve data from
	 * @param suffix
	 *            the suffix of the base-url to request
	 * 
	 * @return the object retrieved or <code>null</code> if no object was
	 *         retrieved
	 */
	public static Object getDeserializedResponse(final int port,
			final String suffix) {

		final byte[] response = getResponse(port, suffix);
		return getDeserialized(response);
	}

	/**
	 * Deserializes the passed {@code response}.
	 * 
	 * @param response
	 *            the object to be deserialized
	 * 
	 * @return the deserialized object
	 */
	public static Object getDeserialized(final byte[] response) {

		final ByteArrayInputStream b = new ByteArrayInputStream(response);
		final ObjectInputStream o;
		try {
			o = new ObjectInputStream(b);

			// get the object
			final Object object = o.readObject();

			// cleanUp
			Streams.closeIO(o);

			// done
			return object;
		} catch (final Exception e) {
			return null;
		} finally {
			Streams.closeIO(b);
		}
	}
}
