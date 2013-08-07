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

	@Before
	public void init() {
		def = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);
	}

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
							+ "'-attribute or define an inner script using the 'scriptfile'-tag.",
					ex.getMessage());
		}
	}

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
							+ "'-attribute or define an inner script using the 'scriptfile'-tag.",
					ex.getMessage());
		}
	}

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

	@Test
	public void testScriptFailure() throws InterruptedException, IOException {

		// create the script-files we will need
		final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		final File script1 = new File(tmpDir, "test_10000.js");
		script1.delete();
		assertTrue(script1.createNewFile());

		String script = "";
		script += "response.setStatusCode(org.apache.http.HttpStatus.SC_OK);";
		script += "var entity = new  org.apache.http.entity.StringEntity('WHAT');";
		script += "response.setEntity(entity);";

		Files.writeToFile(script1, script, "UTF-8");

		// now start the server
		server.startAsync();

		// make sure the server started
		Thread.sleep(100);

		final String responseScript1 = TestHelper.getStringResponse(10000, "");
		System.out.println(responseScript1);

		final String responseScript2 = TestHelper.getStringResponse(10001, "");
		System.out.println(responseScript2);

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
