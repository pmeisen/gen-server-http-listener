package net.meisen.general.server.http.listener;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;

import net.meisen.general.server.http.listener.api.IHandler;
import net.meisen.general.server.listener.utility.AcceptListenerThread;

import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

/**
 * <code>AcceptListenerThread</code> used to accepts requests and handle those
 * with a <code>WorkerThread</code>.
 * 
 * @see WorkerThread
 * 
 * @author pmeisen
 * 
 */
public class RequestListenerThread extends AcceptListenerThread {
	private final HttpService httpService;
	private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;

	/**
	 * Default constructor which specifies the <code>port</code> to listen to
	 * for requests and the <code>handlers</code>, which specify how to handle
	 * the request.
	 * 
	 * @param port
	 *            the port to listen to
	 * @param handlers
	 *            the handlers, which specify how to handle the different
	 *            requests
	 * 
	 * @throws IOException
	 *             if some IO operation fails
	 */
	public RequestListenerThread(final int port,
			final Map<String, IHandler> handlers) throws IOException {
		super(port);

		// Set up the HTTP protocol processor
		final HttpProcessor httpproc = new ImmutableHttpProcessor(
				new HttpResponseInterceptor[] { new ResponseDate(),
						new ResponseServer(), new ResponseContent(),
						new ResponseConnControl() });

		// Set up request handlers
		UriHttpRequestHandlerMapper registry = new UriHttpRequestHandlerMapper();
		for (final Entry<String, IHandler> entry : handlers.entrySet()) {
			registry.register(entry.getKey(), entry.getValue());
		}

		// Set up the HTTP service
		httpService = new HttpService(httpproc, registry);
		connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
	}

	@Override
	protected Thread createWorkerThread(final Socket socket) throws IOException {

		// create the connection
		final HttpServerConnection conn = connFactory.createConnection(socket);

		return new WorkerThread(httpService, conn);
	}
}
