package net.meisen.general.server.http.listener.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Locale;

import net.meisen.general.sbconfigurator.runners.JUnitConfigurationRunner;
import net.meisen.general.sbconfigurator.runners.annotations.ContextClass;
import net.meisen.general.server.Server;
import net.meisen.general.server.http.listener.api.IHandler;
import net.meisen.general.server.http.listener.api.IHandlerManager;
import net.meisen.general.server.http.listener.exceptions.HandlerManagerException;
import net.meisen.general.server.http.listener.handler.DefaultHandlerManager;
import net.meisen.general.server.settings.pojos.Extension;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Tests the implementation of the <code>DefaultHandlerManager</code>.
 * 
 * @author pmeisen
 */
@RunWith(JUnitConfigurationRunner.class)
@ContextClass(Server.class)
public class TestDefaultHandlerManager {

	@Autowired
	@Qualifier("httpListenerHandlerManager")
	private IHandlerManager handlerManager;

	/**
	 * A <code>TestHandler</code> used to check the
	 * <code>DefaultHandlerManager</code>.
	 * 
	 * @author pmeisen
	 * 
	 */
	public static class TestHandler implements IHandler {

		/**
		 * Add onle one constructor
		 * 
		 * @param value
		 *            just a value
		 */
		public TestHandler(final String value) {
			// hidden constructor
		}

		@Override
		public void handle(HttpRequest request, HttpResponse response,
				HttpContext context) throws HttpException, IOException {
			// do nothing
		}

		@Override
		public void initialize(final Extension e) {
			// do nothing
		}
	}

	/**
	 * Check the wiring of the manager and make sure it got some handler wired
	 * as well
	 */
	@Test
	public void testInstantiationViaSpring() {
		assertNotNull(handlerManager);
		assertTrue(handlerManager instanceof DefaultHandlerManager);

		// map to the DefaultHandlerManager
		final DefaultHandlerManager defManager = (DefaultHandlerManager) handlerManager;

		// per default there should be more than none
		assertTrue(defManager.size() > 0);
	}

	/**
	 * Tests the creation of an invalid <code>Handler</code>.
	 */
	@Test
	public void testCreationOfInvalidClass() {
		final Locale def = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);

		final DefaultHandlerManager defManager = (DefaultHandlerManager) handlerManager;
		defManager.addHandler("TEST", TestHandler.class);

		// get a handler
		try {
			defManager.getHandler("TEST");
		} catch (final Exception e) {
			assertTrue(e instanceof HandlerManagerException);
			assertEquals(
					"Unable to instantiate the handler with id 'TEST' and class '"
							+ TestHandler.class.getName()
							+ "'. Is a default constructor available?",
					e.getMessage());
		} finally {
			Locale.setDefault(def);
		}
	}
}
