package net.meisen.general.server.http.listener;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.meisen.general.server.api.impl.BaseListener;
import net.meisen.general.server.http.listener.api.IHandler;
import net.meisen.general.server.http.listener.api.IHandlerManager;
import net.meisen.general.server.listener.utility.AcceptListenerThread;
import net.meisen.general.server.settings.pojos.Connector;
import net.meisen.general.server.settings.pojos.Extension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A <code>Listener</code> which handles HTTP requests.
 * 
 * @author pmeisen
 * 
 */
public class HttpListener extends BaseListener {
	private final static Logger LOG = LoggerFactory
			.getLogger(HttpListener.class);

	/**
	 * The name under which the listener is registered
	 */
	public static final String NAME = "HTTP";
	/**
	 * The urlMatcher used if none is defined
	 */
	public static final String DEF_URLMATCHER = "*";

	@Autowired
	@Qualifier("httpListenerHandlerManager")
	private IHandlerManager handlerManager;

	private final Map<String, IHandler> handlers = new LinkedHashMap<String, IHandler>();

	@Override
	public void initialize(final Connector c) {
		super.initialize(c);

		// get all the handlers defined
		for (final Extension e : c.getExtensions()) {
			final IHandler handler = handlerManager.getHandler(e.getId());
			if (handler == null) {
				if (LOG.isErrorEnabled()) {
					LOG.error("The handler with id '"
							+ e.getId()
							+ "' could not be found, it will be skipped please verify.");
				}
			} else {

				// initialize the handler
				handler.initialize(e);

				// add the handler
				final String urlMatcher = getUrlMatcher(e);
				if (handlers.put(urlMatcher, handler) != null) {
					if (LOG.isErrorEnabled()) {
						LOG.error("There are at least two handlers defined for the urlMatcher '"
								+ urlMatcher
								+ "', please verify it is not defined which handler will be choosen.");
					}
				}
			}
		}

		// warn if no handlers are defined
		if (handlers.size() == 0) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("There aren't any handlers defined, please verify.");
			}
		}
	}

	/**
	 * Get the defined handlers for the instance.
	 * 
	 * @return the defined handlers
	 */
	protected Map<String, IHandler> getHandlers() {
		return Collections.unmodifiableMap(handlers);
	}

	/**
	 * Determines the defined <code>urlMatcher</code> for the specified
	 * <code>Extension</code>. Returns the <code>DEF_URLMATCHER</code> if the
	 * <code>Extension</code> doesn't specify one.
	 * 
	 * @param e
	 *            the <code>Extension</code> to retrieve the
	 *            <code>urlMatcher</code> from
	 * 
	 * @return the determined <code>urlMatcher</code>
	 */
	protected String getUrlMatcher(final Extension e) {
		final String urlMatcher = e.getProperty("urlmatcher");

		if (urlMatcher == null) {
			return DEF_URLMATCHER;
		} else {
			return urlMatcher;
		}

	}

	@Override
	protected AcceptListenerThread createAcceptListenerThread()
			throws IOException {
		return new RequestListenerThread(getPort(), handlers);
	}

	@Override
	public String toString() {
		return NAME + (getPort() == -1 ? "" : " (" + getPort() + ")");
	}
}
