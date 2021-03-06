package net.meisen.general.server.http.listener.api;

import net.meisen.general.server.http.listener.handler.ServletHandler;
import net.meisen.general.server.settings.pojos.Extension;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

/**
 * Interface to define <code>Servlet</code> instances, which are used by the
 * <code>ServletHandler</code> to handle requests.
 * 
 * @see ServletHandler
 * 
 * @author pmeisen
 * 
 */
public interface IServlet {

	/**
	 * Initializes the <code>Servlet</code> passing the defined
	 * <code>Extension</code>.
	 * 
	 * @param e
	 *            the <code>Extension</code> which defines the usage of the
	 *            <code>Servlet</code>
	 */
	public void initialize(final Extension e);

	/**
	 * Method which is called whenever the <code>Servlet</code> is called.
	 * 
	 * @param request
	 *            the <code>HttpRequest</code> send to the <code>Server</code>
	 * @param response
	 *            the <code>HttpResponse</code> to be filled by the
	 *            <code>Servlet</code>
	 * @param context
	 *            the <code>HttpContext</code> of the request
	 */
	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context);

}
