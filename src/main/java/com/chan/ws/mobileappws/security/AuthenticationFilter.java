package com.chan.ws.mobileappws.security;

import com.chan.ws.mobileappws.SpringApplicationContext;
import com.chan.ws.mobileappws.service.UserService;
import com.chan.ws.mobileappws.shared.dto.UserDto;
import com.chan.ws.mobileappws.ui.model.request.UserLoginRequestModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

// when spring framework is trying to authenticate an user, it will use a service (UserServiceImpl) which implements UserService interface which extends UserDetailsService from spring.
// so because of all this, when the user is attempting to login, and spring framework is trying to authenticate our user, a method from UserServiceImpl will be triggered which is called 'loadUserByUsername'.
// so this method will be triggered and the return value of this method is the user object which is currently set with the email and the password.
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;

    public AuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        // this method will be triggered to authenticate the user for login
        try {
            UserLoginRequestModel creds = new ObjectMapper().readValue(request.getInputStream(), UserLoginRequestModel.class);
            // spring framework will do all the work that it needs, it will look up user in our database and for that we have
            // implemented "loadUserByUsername" in "UserServiceImpl" class.
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            creds.getEmail(),
                            creds.getPassword(),
                            new ArrayList<>()
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        String userName = ((User) authResult.getPrincipal()).getUsername();

        String token = Jwts.builder()
                .setSubject(userName)
                .setExpiration(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SecurityConstants.getTokenSecret())
                .compact();
        // Use SpringApplicationContext to get UserServiceImpl bean, so that we can get user details
        UserService userService = (UserService) SpringApplicationContext.getBean("userServiceImpl");

        UserDto userDto =  userService.getUser(userName);

        response.addHeader("UserID", userDto.getUserId());
        response.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token);
    }
}
