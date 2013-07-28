package net.meisen.general.server.http.listener.testutilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
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
				.loadCoreSettings("sbconfigurator-core-useSystemProperties.xml",
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

	public static byte[] getResponse(final int port, final String suffix) {
		final HttpClient httpclient = new DefaultHttpClient();
		final HttpGet httpget = new HttpGet("http://localhost:" + port + "/" + suffix);

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
}