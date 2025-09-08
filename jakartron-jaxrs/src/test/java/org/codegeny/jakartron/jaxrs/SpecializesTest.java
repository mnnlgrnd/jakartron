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

import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.codegeny.jakartron.junit.ExtendWithJakartron;
import org.codegeny.jakartron.servlet.Base;

@ExtendWithJakartron
public class SpecializesTest {

    public static class MyFooResource extends MyResource {

        public MyFooResource() {
            super("foo");
        }
    }

    public static class MyBarResource extends MyResource {

        public MyBarResource() {
            super("bar");
        }
    }

    @ApplicationPath("/api")
    public static class MyApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(MyFooResource.class);
        }

    }

    @Specializes
    @ApplicationPath("/api")
    public static class MyApplication2 extends MyApplication {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(MyBarResource.class);
        }
    }

    @Path("/")
    @Vetoed
    public static class MyResource {

        private final String content;

        public MyResource(String content) {
            this.content = content;
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return content;
        }

    }

    @Test
    public void test(@Base("/api") WebTarget target) {
        assert "bar".equals(target.request().get(String.class));
    }
}
