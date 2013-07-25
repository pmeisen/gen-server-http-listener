package net.meisen.general.server.http.listener;

import java.io.IOException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerThread extends Thread {
	private final static Logger LOG = LoggerFactory.getLogger(WorkerThread.class);

	private final HttpService httpservice;
	private final HttpServerConnection conn;

	public WorkerThread(final HttpService httpservice,
			final HttpServerConnection conn) {
		super();

		this.httpservice = httpservice;
		this.conn = conn;
	}

	@Override
	public void run() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Starting the connection thread...");
		}

		final HttpContext context = new BasicHttpContext(null);
		try {
			while (!Thread.interrupted() && conn.isOpen()) {
				httpservice.handleRequest(conn, context);
			}
		} catch (final ConnectionClosedException ex) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Client closed the connection.", ex);
			}
		} catch (final IOException ex) {
			if (LOG.isErrorEnabled()) {
				LOG.error("I/O error while handling connection.", ex);
			}
		} catch (final HttpException ex) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Unrecoverable HTTP protocol violation.", ex);
			}
		} finally {
			try {
				conn.shutdown();
			} catch (IOException ignore) {
				// ignore
			}
		}
	}

}