package org.codegeny.jakartron.jaxrs;

import jakarta.annotation.Priority;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Priority(Priorities.AUTHENTICATION)
public final class BasicAuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Pattern PATTERN = Pattern.compile("^\\s*basic\\s+([a-z0-9+/]+={0,2})\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Logger LOGGER = Logger.getLogger(BasicAuthenticationFilter.class.getName());

    @Context
    private HttpServletRequest request;

    private final Charset charset;

    public BasicAuthenticationFilter(Charset charset) {
        this.charset = charset;
    }

    public BasicAuthenticationFilter() {
        this(StandardCharsets.UTF_8);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String authorization = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            Matcher matcher = PATTERN.matcher(authorization);
            if (matcher.matches()) {
                String token = new String(Base64.getDecoder().decode(matcher.group(1)), charset);
                int index = token.indexOf(':');
                if (index != -1) {
                    try {
                        request.login(token.substring(0, index), token.substring(index + 1));
                        return; // Success, proceed with request
                    } catch (ServletException exception) {
                        LOGGER.log(Level.FINE, exception, () -> "Could not authenticate " + token);
                    }
                }
            }
        }
        requestContext.abortWith(Response
                                   .status(Response.Status.UNAUTHORIZED)
                                   .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Workflows\" charset=\"" + charset.name() + "\"")
                                   .build()
        );
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        try {
            request.logout();
        } catch (ServletException exception) {
            LOGGER.log(Level.WARNING, "Error while logging out", exception);
        }
        // Might be needed for CSRF tokens.
        //        HttpSession session = request.getSession(false);
        //        if (session != null) {
        //            LOGGER.warning("An HTTP session was created during a stateless call");
        //            for (String name : Collections.list(session.getAttributeNames())) {
        //                LOGGER.warning(() -> String.format("%s => %s", name, session.getAttribute(name)));
        //            }
        //            session.invalidate();
        //        }
    }
}
