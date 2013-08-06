package net.meisen.general.server.http.listener.api;

/**
 * Interface to define the manager used to manage the different available
 * <code>Handler</code>.
 * 
 * @author pmeisen
 * 
 */
public interface IHandlerManager {

	/**
	 * Get the defined <code>Handler</code> for the specified <code>id</code>.
	 * 
	 * @param id
	 *            the id of the <code>Handler</code> to retrieve
	 * @return the <code>Handler</code>, can be <code>null</code> if no
	 *         <code>Handler</code> is registered for the identifier
	 */
	public IHandler getHandler(final String id);
}
