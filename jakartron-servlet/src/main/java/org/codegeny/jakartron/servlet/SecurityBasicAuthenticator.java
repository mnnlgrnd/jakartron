package org.codegeny.jakartron.servlet;

import jakarta.enterprise.inject.Vetoed;

import java.security.Principal;
import java.util.Set;

import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import org.codegeny.jakartron.security.SecurityContextController;

@Vetoed
class SecurityBasicAuthenticator extends BasicAuthenticator {

    private final SecurityContextController securityContextController;

    public SecurityBasicAuthenticator(SecurityContextController securityContextController) {
        this.securityContextController = securityContextController;
    }

    @Override
    public UserIdentity login(String username, Object password, Request request, Response response) {
        UserIdentity userIdentity = super.login(username, password, request, response);
        if (securityContextController != null) {
            securityContextController.setPrincipal(userIdentity.getUserPrincipal());
        }
        return userIdentity;
    }

    @Override
    public void logout(Request request, Response response) {
        request.setAttribute(Principal.class.getName(), null);
        if (securityContextController != null) {
            securityContextController.setPrincipals(Set.of());
        }
        super.logout(request, response);
    }
}
