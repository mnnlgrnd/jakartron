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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;

import org.codegeny.jakartron.security.SecurityContextController;

public class PrincipalProducer {

    public void observe(@Observes @Initialized(ApplicationScoped.class) ServletContext context, SecurityContextController manager) {
        context.addListener(new ServletRequestListener() {

            @Override
            public void requestDestroyed(ServletRequestEvent event) {
                manager.setPrincipal(null);
            }

            @Override
            public void requestInitialized(ServletRequestEvent event) {
                ServletRequest request = event.getServletRequest();
                if (request instanceof HttpServletRequest) {
                    manager.setPrincipal(((HttpServletRequest) request).getUserPrincipal());
                }
            }
        });
    }
}
