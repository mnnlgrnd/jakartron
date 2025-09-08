package org.codegeny.jakartron.jaxrs;

/*-
 * #%L
 * jakartron-jaxrs
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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletRegistration;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.sse.SseEventSource;

import java.lang.annotation.Annotation;
import java.net.URI;

import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

import org.codegeny.jakartron.Internal;
import org.codegeny.jakartron.servlet.Base;
import org.codegeny.jakartron.servlet.Initialized;

@Dependent
public final class JAXRSProducer {

    @Produces
    @ApplicationScoped
    @Internal
    public Client newClient() {
        return ClientBuilder.newClient();
    }

    public void closeClient(@Disposes @Internal Client client) {
        client.close();
    }

    @Produces
    @Base
    public WebTarget newWebTarget(@Internal Client client, InjectionPoint injectionPoint, @Any Instance<URI> uriProvider) {
        return client.target(uriProvider.select(injectionPoint.getQualifiers().toArray(new Annotation[0])).get());
    }

    @Produces
    @Base
    public SseEventSource newSseEventSource(InjectionPoint injectionPoint, @Any Instance<WebTarget> targetProvider) {
        return SseEventSource.target(targetProvider.select(injectionPoint.getQualifiers().toArray(new Annotation[0])).get()).build();
    }

    public void closeSseEventSource(@Disposes @Base SseEventSource sseEventSource) {
        if (sseEventSource.isOpen()) {
            sseEventSource.close();
        }
    }

    public void initializeContext(@Observes @Initialized ServletContextEvent servletContextEvent, BeanManager beanManager) {
        ServletContext context = servletContextEvent.getServletContext();
        context.setInitParameter("resteasy.injector.factory", CdiInjectorFactory.class.getName());
        context.setInitParameter(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS, "true");
        context.setInitParameter(ResteasyContextParameters.RESTEASY_ROLE_BASED_SECURITY, "true");
        beanManager.getBeans(Application.class).stream()
		  .map(Bean::getBeanClass)
		  .filter(applicationClass -> applicationClass.isAnnotationPresent(ApplicationPath.class))
		  .filter(Application.class::isAssignableFrom)
		  .<Class<? extends Application>>map(applicationClass -> applicationClass.asSubclass(Application.class))
		  .forEach(applicationClass -> configureApplication(context, applicationClass));
    }

    private void configureApplication(ServletContext context, Class<? extends Application> applicationClass) {
        String prefix = "/".concat(applicationClass.getAnnotation(ApplicationPath.class).value().replaceAll("^/*|/*$", ""));
        ServletRegistration.Dynamic servlet = context.addServlet("resteasy", HttpServlet30Dispatcher.class);
        servlet.addMapping(prefix.equals("/") ? prefix : prefix.concat("/*"));
		servlet.setInitParameter("jakarta.ws.rs.Application", applicationClass.getName());
        servlet.setInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX, prefix);
        servlet.setMultipartConfig(new MultipartConfigElement(System.getProperty("java.io.tmpdir")));
        servlet.setAsyncSupported(true);
    }
}
