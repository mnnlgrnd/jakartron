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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.codegeny.jakartron.junit.ExtendWithJakartron;
import org.codegeny.jakartron.servlet.Base;
import org.codegeny.jakartron.servlet.DeclareUser;

@ExtendWithJakartron
@DeclareUser(name = "user123", password = "password", roles = "rest-user")
@DeclareUser(name = "user456", password = "password", roles = "not-user")
public class SecuredResourceTest {

    @Path("foo/bar")
    public static class Bar {

        @Inject
        private SecurityContext securityContext;

        @GET
        @RolesAllowed("rest-user")
        public Response bar() {
            return Response.ok(securityContext.getCallerPrincipal().getName()).build();
        }
    }

    @ApplicationPath("api")
    public static class MyApplication extends Application {

        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(Bar.class, BasicAuthenticationFilter.class);
        }
    }

    @Test
    public void testOk(@Base("api") String base) {
        given()
          .baseUri(base)
          .when()
          .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjEyMzpwYXNzd29yZA==")
          .get("foo/bar")
          .then()
          .statusCode(200)
          .body(is("user123"));
    }

    @Test
    public void testForbidden(@Base("api") String base) {
        given()
          .baseUri(base)
          .when()
          .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjQ1NjpwYXNzd29yZA==")
          .get("foo/bar")
          .then()
          .statusCode(403);
    }

    @Test
    public void testUnauthorized(@Base("api") String base) {
        given()
          .baseUri(base)
          .when()
          .get("foo/bar")
          .then()
          .statusCode(401);
    }
}
