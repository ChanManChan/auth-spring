package com.chan.ws.mobileappws.io.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity(name = "password_reset_tokens")
public class PasswordResetTokenEntity implements Serializable {
    private static final long serialVersionUID = 5340373990820730886L;

    @Id
    @GeneratedValue
    private long id;

    private String token;

    @OneToOne // one token can be associated with one user
    @JoinColumn(name = "users_id") // <- this users id will be a foreign key in our password reset token database table
    private UserEntity userDetails;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserEntity getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(UserEntity userDetails) {
        this.userDetails = userDetails;
    }
}
