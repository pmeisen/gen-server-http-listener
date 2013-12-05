package net.meisen.general.server.http.listener.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import net.meisen.general.genmisc.types.Files;
import net.meisen.general.sbconfigurator.api.IConfiguration;
import net.meisen.general.sbconfigurator.runners.JUnitConfigurationRunner;
import net.meisen.general.sbconfigurator.runners.annotations.ContextClass;
import net.meisen.general.sbconfigurator.runners.annotations.ContextFile;
import net.meisen.general.sbconfigurator.runners.annotations.SystemProperty;
import net.meisen.general.server.Server;
import net.meisen.general.server.http.listener.exceptions.ScriptedServletException;
import net.meisen.general.server.http.listener.testutilities.TestHelper;
import net.meisen.general.server.settings.pojos.Extension;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Tests the implementation of the <code>ScriptedServlet</code>.
 * 
 * @author pmeisen
 * 
 */
@RunWith(JUnitConfigurationRunner.class)
@ContextClass(Server.class)
@ContextFile("sbconfigurator-core-useSystemProperties.xml")
@SystemProperty(property = "server.settings.selector", value = "serverHttp-test-scriptedServlets.xml")
public class TestScriptedServlet {

	@Autowired
	@Qualifier(IConfiguration.coreConfigurationId)
	private IConfiguration configuration;

	@Autowired
	@Qualifier("server")
	private Server server;

	private Locale def;

	/**
	 * Initializes the test
	 */
	@Before
	public void init() {
		def = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);
	}
	
	/**
	 * Tests the usage of no script.
	 */
	@Test
	public void testNullScript() {
		final ScriptedServlet servlet = configuration
				.createInstance(ScriptedServlet.class);

		// make sure we have a servlet
		assertNotNull(servlet);

		// create an extension
		final Extension e = new Extension();
		e.setProperty(ScriptedServlet.PROPERTY_SCRIPTFILE, null);

		try {
			servlet.initialize(e);
			fail("Expected exception not thrown");
		} catch (final Exception ex) {
			assertTrue(ex instanceof ScriptedServletException);
			assertEquals(
					"A ScriptedServlet needs the definition of the script to use. Please specify by using the '"
							+ ScriptedServlet.PROPERTY_SCRIPTFILE
							+ "'-attribute or define an inner script using the '"
							+ ScriptedServlet.EXTENSION_SCRIPT + "'-tag.",
					ex.getMessage());
		}
	}

	/**
	 * Tests the definition of an empty script.
	 */
	@Test
	public void testEmptyScript() {
		final ScriptedServlet servlet = configuration
				.createInstance(ScriptedServlet.class);

		// make sure we have a servlet
		assertNotNull(servlet);

		// create an extension
		final Extension e = new Extension();
		e.setProperty(ScriptedServlet.PROPERTY_SCRIPTFILE, "");

		try {
			servlet.initialize(e);
			fail("Expected exception not thrown");
		} catch (final Exception ex) {
			assertTrue(ex instanceof ScriptedServletException);
			assertEquals(
					"A ScriptedServlet needs the definition of the script to use. Please specify by using the '"
							+ ScriptedServlet.PROPERTY_SCRIPTFILE
							+ "'-attribute or define an inner script using the '"
							+ ScriptedServlet.EXTENSION_SCRIPT + "'-tag.",
					ex.getMessage());
		}
	}

	/**
	 * Tests the usage of an invalid <code>File</code>.
	 */
	@Test
	public void testInvalidFile() {
		final ScriptedServlet servlet = configuration
				.createInstance(ScriptedServlet.class);

		// make sure we have a servlet
		assertNotNull(servlet);

		// create an extension
		final Extension e = new Extension();
		e.setProperty(ScriptedServlet.PROPERTY_SCRIPTFILE,
				"myNotExisting.script");

		try {
			servlet.initialize(e);
			fail("Expected exception not thrown");
		} catch (final Exception ex) {
			assertTrue(ex instanceof ScriptedServletException);
			assertEquals(
					"The specified script-file 'myNotExisting.script' cannot be found or read.",
					ex.getMessage());
		}
	}

	/**
	 * Tests the execution of an invalid script.
	 * 
	 * @throws InterruptedException
	 *             if the sleep is interrupted
	 * @throws IOException
	 *             if a script cannot be read
	 */
	@Test
	public void testScriptFailure() throws InterruptedException, IOException {
		// create the script-files we will need
		final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		final File script1 = new File(tmpDir, "test_10000.js");
		script1.delete();
		assertTrue(script1.createNewFile());

		String script = "";
		script += "response.setStatusCode(org.apache.http.HttpStatus.SC_OK);";
		script += "var entity = new StringEntity('AWESOME IT worked on port 10000');";
		script += "response.setEntity(entity);";

		Files.writeToFile(script1, script, "UTF-8");

		// now start the server
		server.startAsync();

		// make sure the server started
		Thread.sleep(100);

		final String responseScript1 = TestHelper.getStringResponse(10000, "");
		assertEquals(
				"<html><body><h1>Servlet Exception</h1><div>Exception while script-execution in line 1 (sun.org.mozilla.javascript.internal.EcmaError: ReferenceError: \"StringEntity\" is not defined. (<Unknown source>#1) in <Unknown source> at line number 1).</div></body></html>",
				responseScript1);

		// shut the server down again
		server.shutdown();
		Thread.sleep(100);

		// cleanUp behind the test
		assertTrue(script1.delete());
	}

	/**
	 * Tests the execution of scripts.
	 * 
	 * @throws InterruptedException
	 *             if the sleep is interrupted
	 * @throws IOException
	 *             if a script cannot be read
	 */
	@Test
	public void testScriptExecution() throws InterruptedException, IOException {

		// create the script-files we will need
		final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		final File script1 = new File(tmpDir, "test_10000.js");
		script1.delete();
		assertTrue(script1.createNewFile());

		String script = "";
		script += "response.setStatusCode(org.apache.http.HttpStatus.SC_OK);";
		script += "var entity = new org.apache.http.entity.StringEntity('AWESOME IT worked on port 10000');";
		script += "response.setEntity(entity);";
		Files.writeToFile(script1, script, "UTF-8");

		// now start the server
		server.startAsync();

		// make sure the server started
		Thread.sleep(100);

		final String responseScript1 = TestHelper.getStringResponse(10000, "");
		assertEquals("AWESOME IT worked on port 10000", responseScript1);
		
		final String responseScript2 = TestHelper.getStringResponse(10001, "");
		assertEquals("THE TEST WAS SUCCESSFUL ON PORT 10001", responseScript2);

		// shut the server down again
		server.shutdown();

		// cleanUp behind the test
		assertTrue(script1.delete());
	}
	
	/**
	 * Reset to use the default <code>Locale</code> again.
	 */
	@After
	public void cleanUp() {
		Locale.setDefault(def);
	}
}
