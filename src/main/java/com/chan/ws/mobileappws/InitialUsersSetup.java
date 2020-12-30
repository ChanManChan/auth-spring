package com.chan.ws.mobileappws;

import com.chan.ws.mobileappws.io.entity.AuthorityEntity;
import com.chan.ws.mobileappws.io.entity.RoleEntity;
import com.chan.ws.mobileappws.io.entity.UserEntity;
import com.chan.ws.mobileappws.io.repositories.AuthorityRepository;
import com.chan.ws.mobileappws.io.repositories.RoleRepository;
import com.chan.ws.mobileappws.io.repositories.UserRepository;
import com.chan.ws.mobileappws.shared.Roles;
import com.chan.ws.mobileappws.shared.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Collection;

@Component //<- this will make spring framework notice this class and thus will create an instance of it at application startup
public class InitialUsersSetup {

    @Autowired
    AuthorityRepository authorityRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    Utils utils;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    UserRepository userRepository;

    @EventListener
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("From Application ready event....");

        AuthorityEntity readAuthority = createAuthority("READ_AUTHORITY");
        AuthorityEntity writeAuthority = createAuthority("WRITE_AUTHORITY");
        AuthorityEntity deleteAuthority = createAuthority("DELETE_AUTHORITY");

        createRole(Roles.ROLE_USER.name(), Arrays.asList(readAuthority, writeAuthority));
        RoleEntity roleAdmin = createRole(Roles.ROLE_ADMIN.name(), Arrays.asList(readAuthority, writeAuthority, deleteAuthority));

        if(roleAdmin == null) return;

        UserEntity adminUser = new UserEntity();
        adminUser.setFirstName("Nanda");
        adminUser.setLastName("Gopal");
        adminUser.setEmail("ngopal253@gmail.com");
        adminUser.setEmailVerificationStatus(true);
        adminUser.setUserId(utils.generateUserId(30));
        adminUser.setEncryptedPassword(bCryptPasswordEncoder.encode("123456"));
        adminUser.setRoles(Arrays.asList(roleAdmin));

        UserEntity savedUser = userRepository.findByEmail(adminUser.getEmail());
        if(savedUser == null) {
            userRepository.save(adminUser);
        }
    }

    @Transactional
    private AuthorityEntity createAuthority(String name) {
            AuthorityEntity authority = authorityRepository.findByName(name);
        if(authority == null) {
            authority = new AuthorityEntity(name);
            authorityRepository.save(authority);
        }
        return authority;
    }

    @Transactional
    private RoleEntity createRole(String name, Collection<AuthorityEntity> authorities) {
        RoleEntity role = roleRepository.findByName(name);
        if(role == null) {
            role = new RoleEntity(name);
            role.setAuthorities(authorities);
            roleRepository.save(role);
        }
        return role;
    }
}
