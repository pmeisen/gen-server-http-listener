package net.meisen.general.server.http.listener.exceptions;

/**
 * A <code>HttpListenerException</code> is thrown whenever the listener failed
 * to initialize or to open the connection.
 * 
 * @author pmeisen
 * 
 */
public class HttpListenerException extends RuntimeException {
	private static final long serialVersionUID = 9041793151940914571L;

	/**
	 * Creates an exception which should been thrown whenever there is no other
	 * reason for the exception, i.e. the exception is the root.
	 * 
	 * @param message
	 *          the message of the exception
	 */
	public HttpListenerException(final String message) {
		super(message);
	}

	/**
	 * Creates an exception which should been thrown whenever another
	 * <code>Throwable</code> is the reason for this.
	 * 
	 * @param message
	 *          the message of the exception
	 * @param t
	 *          the reason for the exception
	 */
	public HttpListenerException(final String message, final Throwable t) {
		super(message, t);
	}
}
