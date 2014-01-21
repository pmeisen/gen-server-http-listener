package net.meisen.general.server.http.listener.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import net.meisen.general.genmisc.exceptions.registry.IExceptionRegistry;
import net.meisen.general.sbconfigurator.api.IConfiguration;
import net.meisen.general.server.http.listener.api.IHandler;
import net.meisen.general.server.http.listener.api.IHandlerManager;
import net.meisen.general.server.http.listener.exceptions.HandlerManagerException;

/**
 * The default <code>HandlerManager</code>, which contains all the defined
 * <code>Handler</code> definitions.
 * 
 * <pre>
 * &lt;!-- specific handlers can be added using the sbconfigurator-bean.xml --&gt;
 * &lt;bean class=&quot;org.springframework.beans.factory.config.MethodInvokingFactoryBean&quot;&gt;
 *   &lt;property name=&quot;targetObject&quot;&gt;&lt;ref bean=&quot;httpListenerHandlerManager&quot; /&gt;&lt;/property&gt;
 *   &lt;property name=&quot;targetMethod&quot; value=&quot;addHandlers&quot; /&gt;
 *   &lt;property name=&quot;arguments&quot;&gt;
 *     &lt;map key-type=&quot;java.lang.String&quot; value-type=&quot;java.lang.Class&quot;&gt;
 *       &lt;!-- the key is the tag which identifies the usage of the handler --&gt;
 *       &lt;entry key=&quot;handler&quot; value=&quot;hello.world.MyHandler&quot; /&gt;
 *       &lt;!-- ... --&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * @see IHandlerManager
 * @see IHandler
 * 
 * @author pmeisen
 * 
 */
public class DefaultHandlerManager implements IHandlerManager {

	private Map<String, Class<? extends IHandler>> content = new HashMap<String, Class<? extends IHandler>>();

	@Autowired
	@Qualifier(IConfiguration.coreExceptionRegistryId)
	private IExceptionRegistry exceptionRegistry;

	@Autowired
	@Qualifier(IConfiguration.coreConfigurationId)
	private IConfiguration configuration;

	/**
	 * Add a <code>Handler</code> to the manager.
	 * 
	 * @param id
	 *            the identifier to associate the <code>Handler</code> with
	 * @param clazz
	 *            the class of the <code>Handler</code> to be associated
	 */
	public void addHandler(final String id,
			final Class<? extends IHandler> clazz) {
		content.put(id.toUpperCase(), clazz);
	}

	/**
	 * Adds several handlers at once.
	 * 
	 * @param handlers
	 *            the <code>Handler</code> to add
	 */
	public void addHandlers(
			final Map<String, Class<? extends IHandler>> handlers) {
		for (final Entry<String, Class<? extends IHandler>> entry : handlers
				.entrySet()) {
			addHandler(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public IHandler getHandler(final String id) {
		if (id == null) {
			return null;
		} else {
			final Class<? extends IHandler> handlerClazz = content.get(id
					.toUpperCase());

			// check if we got an instance
			if (handlerClazz == null) {
				return null;
			} else {
				try {
					return configuration.createInstance(handlerClazz);
				} catch (final Exception e) {
					exceptionRegistry.throwException(
							HandlerManagerException.class, 1000, id,
							handlerClazz.getName());

					// will never happen
					return null;
				}
			}
		}
	}

	/**
	 * The amount of currently managed <code>Handlers</code>.
	 * 
	 * @return the amount of managed <code>Handlers</code>
	 */
	public int size() {
		return content.size();
	}
}
