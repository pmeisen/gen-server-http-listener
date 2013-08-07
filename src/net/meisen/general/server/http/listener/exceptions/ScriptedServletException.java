package net.meisen.general.server.http.listener.exceptions;

/**
 * Exception thrown whenever a problem with a <code>ScriptedServlet</code>
 * occurs.
 * 
 * @author pmeisen
 * 
 */
public class ScriptedServletException extends RuntimeException {
	private static final long serialVersionUID = 6978593150573025483L;

	/**
	 * Creates an exception which should been thrown whenever there is no other
	 * reason for the exception, i.e. the exception is the root.
	 * 
	 * @param message
	 *            the message of the exception
	 */
	public ScriptedServletException(final String message) {
		super(message);
	}

	/**
	 * Creates an exception which should been thrown whenever another
	 * <code>Throwable</code> is the reason for this.
	 * 
	 * @param message
	 *            the message of the exception
	 * @param t
	 *            the reason for the exception
	 */
	public ScriptedServletException(final String message, final Throwable t) {
		super(message, t);
	}
}
