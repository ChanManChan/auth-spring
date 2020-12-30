package com.chan.ws.mobileappws.security;

import com.chan.ws.mobileappws.io.entity.UserEntity;
import com.chan.ws.mobileappws.io.repositories.UserRepository;
import io.jsonwebtoken.Jwts;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

// Add this filter to WebSecurity filter chain inside the configure method
// this is used when the user is performing http request that requires authorization token to be provided
// eg- when user is performing DELETE we require that authorization token to be provided in the http request
public class AuthorizationFilter extends BasicAuthenticationFilter {
    private final UserRepository userRepository;
    public AuthorizationFilter(AuthenticationManager authManager, UserRepository userRepository) {
        super(authManager);
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader(SecurityConstants.HEADER_STRING);

        if(header == null || !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            // continue to the next filter we have in the chain
            chain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.HEADER_STRING);

        if(token != null) {
            token = token.replace(SecurityConstants.TOKEN_PREFIX, "");

            String user = Jwts.parser()
                    .setSigningKey(SecurityConstants.getTokenSecret())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            if(user != null) {
                UserEntity userEntity = userRepository.findByEmail(user);

                if(userEntity == null) return null;

                UserPrincipal userPrincipal = new UserPrincipal(userEntity);
                // return new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                // return new UsernamePasswordAuthenticationToken(user, null, userPrincipal.getAuthorities());
                // done because of @PreAuthorize("hasRole('ADMIN') or #id == principal.id") in user controller
                return new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
            }
            return null;
        }
        return  null;
    }
}
