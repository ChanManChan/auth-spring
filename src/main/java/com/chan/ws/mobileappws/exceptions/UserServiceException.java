package com.chan.ws.mobileappws.exceptions;

public class UserServiceException extends RuntimeException {

    private static final long serialVersionUID = 6041705398019353812L;

    public UserServiceException(String message) {
        super(message);
    }
}
