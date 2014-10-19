package net.meisen.general.server.http.listener;

import java.io.IOException;
import java.net.Socket;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>Thread</code> used to handle requests.
 * 
 * @author pmeisen
 * 
 */
public class WorkerThread extends
		net.meisen.general.server.listener.utility.WorkerThread {
	private final static Logger LOG = LoggerFactory
			.getLogger(WorkerThread.class);

	private final HttpService httpService;
	private final HttpServerConnection conn;

	/**
	 * Default constructor which specifies the <code>HttpService</code> and the
	 * <code>HttpServerConnection</code>.
	 * 
	 * @param httpService
	 *            the <code>HttpService</code> to be used
	 * @param conn
	 *            the <code>HttpServerConnection</code> to use
	 * @param socket
	 *            the socket used for the connection
	 * 
	 * @see HttpService
	 * @see HttpServerConnection
	 */
	public WorkerThread(final HttpService httpService,
			final HttpServerConnection conn, final Socket socket) {
		super(socket);

		this.httpService = httpService;
		this.conn = conn;
	}

	@Override
	public void run() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Starting the connection thread...");
		}

		final HttpContext context = new BasicHttpContext(null);
		try {
			while (!Thread.interrupted() && this.conn.isOpen()) {
				httpService.handleRequest(conn, context);
			}
		} catch (final ConnectionClosedException ex) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Client closed the connection.");
			}
		} catch (final IOException ex) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("I/O error while handling connection (more details on TRACE level).");
			} else if (LOG.isTraceEnabled()) {
				LOG.trace("I/O error while handling connection.", ex);
			}
		} catch (final HttpException ex) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Unrecoverable HTTP protocol violation.", ex);
			}
		} finally {
			try {
				conn.shutdown();
			} catch (final IOException ignore) {
				// ignore
			}
		}
	}
}