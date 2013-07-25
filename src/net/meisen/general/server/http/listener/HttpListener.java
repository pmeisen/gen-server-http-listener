package net.meisen.general.server.http.listener;

import java.io.IOException;
import java.net.BindException;

import net.meisen.general.genmisc.exceptions.registry.IExceptionRegistry;
import net.meisen.general.genmisc.types.Files;
import net.meisen.general.server.api.IListener;
import net.meisen.general.server.http.listener.exceptions.HttpListenerException;
import net.meisen.general.server.settings.pojos.Connector;
import net.meisen.general.server.settings.pojos.Extension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class HttpListener implements IListener {
	private final static Logger LOG = LoggerFactory.getLogger(HttpListener.class);

	/**
	 * The name under which the listener is registered
	 */
	public static String NAME = "HTTP";

	private int port = -1;
	private String docRoot;

	private RequestListenerThread listenerThread;

	@Autowired
	@Qualifier("exceptionRegistry")
	private IExceptionRegistry exceptionRegistry;

	@Override
	public void initialize(final Connector c) {

		// get the root of the documents
		final Extension docRootExtension = c.getExtension("docroot");
		if (docRootExtension == null) {
			exceptionRegistry.throwException(HttpListenerException.class, 1002);
		}
		final String specDocRoot = c.getExtension("docroot").getProperty("");
		if (specDocRoot == null || specDocRoot.matches("\\s*")) {
			exceptionRegistry.throwException(HttpListenerException.class, 1002);
		} else if (Files.checkDirectory(specDocRoot) == null) {
			exceptionRegistry.throwException(HttpListenerException.class, 1003,
					Files.getCanonicalPath(specDocRoot));
		}

		// get the specified port
		final int specPort = c.getPort();
		if (specPort < 1 || specPort > 65535) {
			exceptionRegistry.throwException(HttpListenerException.class, 1000,
					specPort);
		}

		// set the values
		this.port = specPort;
		this.docRoot = Files.getCanonicalPath(specDocRoot.trim());

		// log the success
		if (LOG.isDebugEnabled()) {
			LOG.debug("Initialized '" + toString() + "' at '" + docRoot + "'");
		}
	}

	@Override
	public void open() {

		// check if we have a running thread
		if (listenerThread != null) {
			exceptionRegistry.throwException(HttpListenerException.class, 1004,
					toString());
		}

		// log it
		if (LOG.isTraceEnabled()) {
			LOG.trace("Opening '" + toString() + "...");
		}

		// start the new thread
		try {
			listenerThread = new RequestListenerThread(port, getDocRoot());
		} catch (final BindException e) {
			exceptionRegistry.throwException(HttpListenerException.class, 1005, e,
					port);
		} catch (final IOException e) {
			exceptionRegistry.throwException(HttpListenerException.class, 1001, e);
		}

		// run the thread
		listenerThread.setDaemon(false);
		listenerThread.start();
	}

	@Override
	public void close() {

		if (listenerThread != null && !listenerThread.isClosed()) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Closing '" + toString() + "'...");
			}

			// mark it to be interrupted and then forget about it
			listenerThread.close();
			listenerThread = null;
		}
	}

	public boolean isClosed() {
		return listenerThread == null || listenerThread.isClosed();
	}

	public String getDocRoot() {
		return docRoot;
	}

	@Override
	public String toString() {
		return NAME + (port == -1 ? "" : " (" + port + ")");
	}

	public int getPort() {
		return port;
	}
}
