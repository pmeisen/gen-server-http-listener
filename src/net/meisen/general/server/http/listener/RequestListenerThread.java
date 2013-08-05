package net.meisen.general.server.http.listener;

import java.io.IOException;
import java.net.Socket;

import net.meisen.general.server.listener.utility.AcceptListenerThread;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestListenerThread extends AcceptListenerThread {
	private final static Logger LOG = LoggerFactory
			.getLogger(RequestListenerThread.class);

	private final HttpParams params;
	private final HttpService httpService;

	public RequestListenerThread(final int port, final String docroot)
			throws IOException {
		super(port);

		params = new SyncBasicHttpParams();
		params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
				.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,
						8 * 1024)
				.setBooleanParameter(
						CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
				.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
				.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "");

		// Set up the HTTP protocol processor
		final HttpProcessor httpproc = new ImmutableHttpProcessor(
				new HttpResponseInterceptor[] { new ResponseDate(),
						new ResponseServer(), new ResponseContent(),
						new ResponseConnControl() });

		// Set up request handlers
		final HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
		registry.register("*", new HttpFileHandler(docroot));

		// Set up the HTTP service
		httpService = new HttpService(httpproc,
				new DefaultConnectionReuseStrategy(),
				new DefaultHttpResponseFactory(), registry, params);
	}

	@Override
	protected Thread createWorkerThread(final Socket socket) throws IOException {

		// create the connection
		final DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
		conn.bind(socket, params);

		return new WorkerThread(httpService, conn);
	}
}
