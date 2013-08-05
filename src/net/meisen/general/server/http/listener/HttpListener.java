package net.meisen.general.server.http.listener;

import java.io.IOException;

import net.meisen.general.genmisc.exceptions.registry.IExceptionRegistry;
import net.meisen.general.genmisc.types.Files;
import net.meisen.general.server.api.impl.BaseListener;
import net.meisen.general.server.http.listener.exceptions.HttpListenerException;
import net.meisen.general.server.listener.utility.AcceptListenerThread;
import net.meisen.general.server.settings.pojos.Connector;
import net.meisen.general.server.settings.pojos.Extension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class HttpListener extends BaseListener {
	private final static Logger LOG = LoggerFactory
			.getLogger(HttpListener.class);

	/**
	 * The name under which the listener is registered
	 */
	public static String NAME = "HTTP";

	private String docRoot;

	@Autowired
	@Qualifier("exceptionRegistry")
	private IExceptionRegistry exceptionRegistry;

	@Override
	public void initialize(final Connector c) {
		super.initialize(c);

		// get the root of the documents
		final Extension docRootExtension = c.getExtension("docroot");
		if (docRootExtension == null) {
			exceptionRegistry.throwException(HttpListenerException.class, 1000);
		}
		final String specDocRoot = c.getExtension("docroot").getProperty("");
		if (specDocRoot == null || specDocRoot.matches("\\s*")) {
			exceptionRegistry.throwException(HttpListenerException.class, 1000);
		} else if (Files.checkDirectory(specDocRoot) == null) {
			exceptionRegistry.throwException(HttpListenerException.class, 1001,
					Files.getCanonicalPath(specDocRoot));
		}

		// set the values
		this.docRoot = Files.getCanonicalPath(specDocRoot.trim());

		// log the success
		if (LOG.isDebugEnabled()) {
			LOG.debug("Initialized '" + toString() + "' at '" + docRoot + "'");
		}
	}

	@Override
	protected AcceptListenerThread createAcceptListenerThread()
			throws IOException {
		return new RequestListenerThread(getPort(), getDocRoot());
	}

	public String getDocRoot() {
		return docRoot;
	}

	@Override
	public String toString() {
		return NAME + (getPort() == -1 ? "" : " (" + getPort() + ")");
	}
}
