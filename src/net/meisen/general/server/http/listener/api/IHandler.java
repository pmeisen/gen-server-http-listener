package net.meisen.general.server.http.listener.api;

import net.meisen.general.server.settings.pojos.Extension;

import org.apache.http.protocol.HttpRequestHandler;

/**
 * Handler interface to handle specific request, which are sent to the
 * <code>Server</code>.
 * 
 * @author pmeisen
 * 
 */
public interface IHandler extends HttpRequestHandler {

	/**
	 * Initializes the <code>Handler</code> passing the defined
	 * <code>Extension</code>.
	 * 
	 * @param e
	 *            the <code>Extension</code> which defines the usage of the
	 *            <code>Handler</code>
	 */
	public void initialize(final Extension e);
}
