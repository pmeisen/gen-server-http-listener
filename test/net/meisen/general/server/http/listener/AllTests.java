package net.meisen.general.server.http.listener;

import net.meisen.general.server.http.listener.handler.TestDefaultHandlerManager;
import net.meisen.general.server.http.listener.handler.TestFileHandler;
import net.meisen.general.server.http.listener.handler.TestServletHandler;
import net.meisen.general.server.http.listener.servlets.TestScriptedServlet;
import net.meisen.general.server.http.listener.util.TestRequestHandlingUtilities;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * All tests together as a {@link Suite}
 * 
 * @author pmeisen
 * 
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ TestDefaultHandlerManager.class,
		TestHttpListenerRegistration.class, TestHttpListenerControl.class,
		TestFileHandler.class, TestServletHandler.class,
		TestRequestHandlingUtilities.class, TestScriptedServlet.class })
public class AllTests {
	// nothing more to do here
}
