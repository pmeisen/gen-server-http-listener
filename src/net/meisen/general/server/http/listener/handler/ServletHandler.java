package net.meisen.general.server.http.listener.handler;

import net.meisen.general.genmisc.exceptions.registry.IExceptionRegistry;
import net.meisen.general.genmisc.types.Classes;
import net.meisen.general.sbconfigurator.api.IConfiguration;
import net.meisen.general.server.http.listener.api.IHandler;
import net.meisen.general.server.http.listener.api.IServlet;
import net.meisen.general.server.http.listener.exceptions.ServletHandlerException;
import net.meisen.general.server.settings.pojos.Extension;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;

/**
 * Handler to handle servlet like requests, i.e. a servlet is a Java
 * implementation which is used to handle the request.
 * <p>
 * A ServletHandler needs to be configured within the serverSettings (normally
 * <code>serverSettings.xml</code>).
 * <p>
 * <pre>
 * &lt;connector port=&quot;666&quot; listener=&quot;HTTP&quot;&gt;
 *   &lt;e:extension&gt;
 *     &lt;servlet&gt;hello.world.MyServlet&lt;/servlet&gt;
 *   &lt;/e:extension&gt;
 * &lt;/connector&gt;
 * </pre>
 *
 * @author pmeisen
 */
public class ServletHandler implements IHandler {
    private final static Logger LOG = LoggerFactory
            .getLogger(ServletHandler.class);

    @Autowired
    @Qualifier(IConfiguration.coreExceptionRegistryId)
    private IExceptionRegistry exceptionRegistry;

    @Autowired
    @Qualifier(IConfiguration.coreConfigurationId)
    private IConfiguration configuration;

    private IServlet servlet;

    @Override
    public void initialize(final Extension e) {
        String servletClazzName;

        // if no extension is defined we use the default
        if (e == null) {
            exceptionRegistry.throwException(ServletHandlerException.class, 1000);
        } else if ((servletClazzName = e.getProperty("")) != null) {

            // remove any whitespaces
            servletClazzName = servletClazzName.trim();

            // get the class
            final Class<?> definedServletClazz = Classes
                    .getClass(servletClazzName);
            if (definedServletClazz == null) {
                exceptionRegistry.throwException(ServletHandlerException.class,
                        1001, servletClazzName);
            } else if (!IServlet.class.isAssignableFrom(definedServletClazz)) {
                exceptionRegistry.throwException(ServletHandlerException.class,
                        1002, servletClazzName);
            } else {
                final IServlet servlet = (IServlet) configuration
                        .createInstance(definedServletClazz);

                // initialize the Servlet
                try {
                    servlet.initialize(e);
                } catch (final Exception ex) {
                    exceptionRegistry.throwException(ServletHandlerException.class, 1004, ex);
                }

                // keep it
                this.servlet = servlet;
            }
        } else {
            exceptionRegistry.throwException(ServletHandlerException.class, 1000);
        }
    }

    @Override
    public void handle(final HttpRequest request, final HttpResponse response,
                       final HttpContext context) throws HttpException, IOException {

        // check if it has been initialized
        if (servlet == null) {
            exceptionRegistry.throwException(ServletHandlerException.class,
                    1003);
        }

        // handle the request
        try {
            servlet.handle(request, response, context);
        }
        // make sure that nothing can stop the servlet
        catch (final Throwable t) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to execute servlet '"
                        + servlet.getClass().getName() + "'", t);
            }

            // answer the client
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            StringEntity entity = new StringEntity(
                    "<html><body><h1>Servlet Exception</h1><div>"
                            + t.getLocalizedMessage() + "</div></body></html>",
                    ContentType.create("text/html", "UTF-8"));
            response.setEntity(entity);
        }
    }
}
