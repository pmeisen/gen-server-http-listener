package net.meisen.general.server.http.listener;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * All tests together as a {@link Suite}
 * 
 * @author pmeisen
 * 
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ TestHttpListenerRegistration.class,
		TestHttpListenerControl.class, TestHttpListenerFileHandler.class })
public class AllTests {
	// nothing more to do here
}
