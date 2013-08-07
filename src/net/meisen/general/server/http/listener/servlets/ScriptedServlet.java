package net.meisen.general.server.http.listener.servlets;

import java.io.File;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.meisen.general.genmisc.exceptions.registry.IExceptionRegistry;
import net.meisen.general.genmisc.types.Files;
import net.meisen.general.server.http.listener.api.IServlet;
import net.meisen.general.server.http.listener.exceptions.ScriptedServletException;
import net.meisen.general.server.settings.pojos.Extension;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Servlet which is scripted.
 * 
 * @author pmeisen
 * 
 */
public class ScriptedServlet implements IServlet {
	private final static Logger LOG = LoggerFactory
			.getLogger(ScriptedServlet.class);

	/**
	 * The default ScriptEngine to be used
	 */
	public final static String DEF_SCRIPTENGINE = "JavaScript";
	/**
	 * The property to specify the script-file with
	 */
	public final static String PROPERTY_SCRIPTFILE = "scriptfile";
	/**
	 * The extension used to define a script
	 */
	public final static String EXTENSION_SCRIPT = "scriptfile";

	@Autowired(required = false)
	private ScriptEngine engine = null;

	@Autowired
	@Qualifier("exceptionRegistry")
	private IExceptionRegistry exceptionRegistry;

	private String script;

	@Override
	public void initialize(final Extension e) {

		// create the engine
		if (engine == null) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("No specific engine specified, using default engine '"
						+ DEF_SCRIPTENGINE + "'");
			}

			final ScriptEngineManager factory = new ScriptEngineManager();
			engine = factory.getEngineByName(DEF_SCRIPTENGINE);
		}

		// get the script to be used
		final String script;
		if (e.hasExtension(EXTENSION_SCRIPT)) {
			final Extension scriptExtension = e.getExtension(EXTENSION_SCRIPT);
			script = scriptExtension.getProperty("");
		} else {
			final String scriptFileName = e.getProperty(PROPERTY_SCRIPTFILE);
			if (scriptFileName == null || "".equals(scriptFileName.trim())) {
				script = "";
			} else {
				final File scriptFile = new File(scriptFileName);
				if (!scriptFile.exists() || !scriptFile.isFile()
						|| !scriptFile.canRead()) {
					exceptionRegistry.throwException(
							ScriptedServletException.class, 1001, scriptFile);
				}
				try {
					script = Files.readFromFile(scriptFile);
				} catch (final Exception ex) {
					exceptionRegistry.throwException(
							ScriptedServletException.class, 1002, ex,
							scriptFile);
					return;
				}
			}
		}

		// check if there is script defined
		if (script == null || "".equals(script.trim())) {
			exceptionRegistry.throwException(ScriptedServletException.class,
					1000, PROPERTY_SCRIPTFILE, EXTENSION_SCRIPT);
		}

		this.script = script;
	}

	@Override
	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) {

		// remove all bindings for the engine
		engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

		// add the bindings
		engine.put("request", request);
		engine.put("response", response);
		engine.put("context", context);
		System.out.println("----> " + this.script);
		try {
			engine.eval(this.script);
		} catch (final ScriptException ex) {
			exceptionRegistry.throwException(ScriptedServletException.class,
					1003, ex, ex.getLineNumber());
		}
	}
}
