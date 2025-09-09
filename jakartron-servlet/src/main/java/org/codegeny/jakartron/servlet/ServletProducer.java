package org.codegeny.jakartron.servlet;

/*-
 * #%L
 * jakartron-servlet
 * %%
 * Copyright (C) 2018 - 2021 Codegeny
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Singleton;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.apache.tomcat.util.scan.Constants;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.webapp.Configurations;
import org.eclipse.jetty.ee10.webapp.JaspiConfiguration;
import org.eclipse.jetty.ee10.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.ee10.webapp.JspConfiguration;
import org.eclipse.jetty.ee10.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.security.Credential;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.module.web.servlet.WeldInitialListener;
import org.jboss.weld.module.web.servlet.WeldTerminalListener;

import org.codegeny.jakartron.CoreExtension;
import org.codegeny.jakartron.security.SecurityContextController;

/**
 * Simple Servlet integration which starts/stops a jetty server on a random available port and fire a ServletContext
 * event for dynamic registration of servlets, filters, etc. The base URI of the server is also published as a bean and
 * can be retrieved through regular injection.
 */
final class ServletProducer {

    static {
        System.setProperty(Constants.SKIP_JARS_PROPERTY, "xalan-*.jar,serializer-*.jar");
    }

    private static final Logger LOGGER = Logger.getLogger(ServletProducer.class.getName());

    @Produces
    @Base
    public URI uri(InjectionPoint injectionPoint, Server server) {
        return injectionPoint.getQualifiers().stream()
          .filter(Base.class::isInstance)
          .map(Base.class::cast)
          .map(Base::value)
          .map(server.getURI()::resolve)
          .findFirst()
          .orElseThrow(InternalError::new);
    }

    @Produces
    @Base
    public String string(InjectionPoint injectionPoint, Server server) {
        return uri(injectionPoint, server).toASCIIString();
    }

    @Produces
    @Base
    public URL url(InjectionPoint injectionPoint, Server server) throws MalformedURLException {
        return uri(injectionPoint, server).toURL();
    }

    @Produces
    @Singleton
    private Server startServer(WebAppContext webAppContext, BeanManager beanManager) throws Exception {
        Server server = new Server(0);
        Configurations.setServerDefault(server);
        server.setHandler(webAppContext);
        server.start();
        beanManager.getExtension(CoreExtension.class).addShutdownHook(server::stop);
        LOGGER.info(() -> String.format("Started server on %s", server.getURI()));
        return server;
    }

    @Produces
    private WebAppContext webAppContext(BeanManager beanManager, LoginService loginService, LoginAuthenticator authenticator) throws Exception {
        ResourceFactory factory = ResourceFactory.root();
        List<Resource> baseResources = new ArrayList<>();

        baseResources.add(factory.newClassLoaderResource("/", false));
        baseResources.add(factory.newClassLoaderResource("/META-INF/resources", false));
        File user = new File(System.getProperty("user.dir"));
        File main = new File(user, "src/main/webapp");
        if (main.exists() && main.isDirectory()) {
            baseResources.add(factory.newResource(main.toPath()));
        }
        File test = new File(user, "src/test/webapp");
        if (test.exists() && test.isDirectory()) {
            baseResources.add(factory.newResource(test.toPath()));
        }

        baseResources.removeIf(Objects::isNull);
        WebAppContext webAppContext = new WebAppContext(Paths.get(System.getProperty("java.io.tmpdir")).toUri().toASCIIString(), "/");
        webAppContext.addConfiguration(new JettyWebXmlConfiguration(), new JspConfiguration(), new JaspiConfiguration());
        webAppContext.setBaseResource(ResourceFactory.combine(baseResources));
        webAppContext.setAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN, ".*taglibs-standard-impl-.*\\.jar$");
        webAppContext.setAttribute(MetaInfConfiguration.WEBINF_JAR_PATTERN, ".*taglibs-standard-impl-.*\\.jar$");
        webAppContext.addEventListener(new WeldInitialListener(BeanManagerProxy.unwrap(beanManager)));
        webAppContext.addEventListener(new BridgingServletContextListener(beanManager));
        webAppContext.getSecurityHandler().setLoginService(loginService);
        webAppContext.getSecurityHandler().setAuthenticator(authenticator);
        webAppContext.addServletContainerInitializer((set, servletContext) -> servletContext.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager()));
        webAppContext.addServlet(JspServlet.class, "*.jsp");
        webAppContext.addServlet(DefaultServlet.class, "/");
        beanManager.getEvent().select(WebAppContext.class).fire(webAppContext);
        webAppContext.addEventListener(new WeldTerminalListener(BeanManagerProxy.unwrap(beanManager)));
        return webAppContext;
    }

    @Produces
    public LoginService loginService(UserStore userStore) {
        HashLoginService loginService = new HashLoginService();
        loginService.setUserStore(userStore);
        return loginService;
    }

    @Produces
    public LoginAuthenticator authenticator(SecurityContextController securityContextController) {
        return new SecurityBasicAuthenticator(securityContextController);
    }

    @Produces
    public UserStore userStore(Event<SecurityConfigurationEvent> event) {
        UserStore userStore = new UserStore();
        event.fire((name, credentials, roles) -> userStore.addUser(name, new PlainCredentials(credentials), roles));
        return userStore;
    }

    private static final class PlainCredentials extends Credential {

        private final String password;

        PlainCredentials(String password) {
            this.password = password;
        }

        @Override
        public boolean check(Object credentials) {
            return password.equals(credentials);
        }
    }

}
