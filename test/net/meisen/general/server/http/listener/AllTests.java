package net.meisen.general.server.http.listener;

import net.meisen.general.server.http.listener.handler.TestDefaultHandlerManager;
import net.meisen.general.server.http.listener.handler.TestFileHandler;

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
		TestFileHandler.class })
public class AllTests {
	// nothing more to do here
}
