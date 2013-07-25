package net.meisen.general.server.http.listener;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

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

public class RequestListenerThread extends Thread {
	private final static Logger LOG = LoggerFactory
			.getLogger(RequestListenerThread.class);

	private final ServerSocket serversocket;
	private final HttpParams params;
	private final HttpService httpService;

	public RequestListenerThread(final int port, final String docroot)
			throws IOException {
		serversocket = new ServerSocket(port);
		params = new SyncBasicHttpParams();
		params
				.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
				.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
				.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
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
				new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory(),
				registry, params);
	}

	@Override
	public void run() {

		// determine the status of the first call
		final boolean firstState = !Thread.interrupted() && !isClosed();
		boolean curState = firstState;

		// do while incoming connections can be accepted
		while (curState) {
			if (LOG.isInfoEnabled()) {
				LOG.info("Start listening on port " + serversocket.getLocalPort()
						+ "...");
			}

			try {
				// listen to the socket
				final Socket socket = serversocket.accept();

				// log the incoming connection
				if (LOG.isDebugEnabled()) {
					LOG.debug("Incoming connection from " + socket.getInetAddress());
				}

				// create the connection
				final DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
				conn.bind(socket, params);

				// start the thread to handle the connection
				final Thread t = new WorkerThread(httpService, conn);
				t.setDaemon(true);
				t.start();

				// set the current state
				curState = !Thread.interrupted() && !isClosed();
			} catch (final InterruptedIOException ex) {
				break;
			} catch (final SocketException e) {
				break;
			} catch (final IOException e) {
				if (LOG.isErrorEnabled()) {
					LOG.error("I/O error initialising connection thread", e);
				}
				break;
			}
		}

		if (firstState) {
			if (LOG.isInfoEnabled()) {
				LOG.info("End listening on port " + serversocket.getLocalPort() + "...");
			}

			// make sure the socket is closed
			close();
		}
	}

	@Override
	public void interrupt() {
		super.interrupt();

		// when the thread is interrupted we should also close the socket
		close();
	}

	/**
	 * Closes the listener and makes sure that no further connections are handled.
	 */
	public void close() {

		synchronized (serversocket) {

			// close the connection
			if (!serversocket.isClosed()) {
				try {
					serversocket.close();
				} catch (final IOException e) {
					// ignore it
				}
			}
		}
	}

	public boolean isClosed() {
		synchronized (serversocket) {
			return serversocket.isClosed();
		}
	}
}
