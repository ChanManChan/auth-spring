package com.chan.ws.mobileappws.service.impl;

import com.chan.ws.mobileappws.exceptions.UserServiceException;
import com.chan.ws.mobileappws.io.entity.PasswordResetTokenEntity;
import com.chan.ws.mobileappws.io.entity.RoleEntity;
import com.chan.ws.mobileappws.io.repositories.PasswordResetTokenRepository;
import com.chan.ws.mobileappws.io.repositories.RoleRepository;
import com.chan.ws.mobileappws.io.repositories.UserRepository;
import com.chan.ws.mobileappws.io.entity.UserEntity;
import com.chan.ws.mobileappws.security.UserPrincipal;
import com.chan.ws.mobileappws.service.UserService;
import com.chan.ws.mobileappws.shared.AmazonSES;
import com.chan.ws.mobileappws.shared.Utils;
import com.chan.ws.mobileappws.shared.dto.AddressDTO;
import com.chan.ws.mobileappws.shared.dto.UserDto;
import com.chan.ws.mobileappws.ui.model.response.ErrorMessages;
import com.fasterxml.jackson.databind.util.BeanUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Service
public class  UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    Utils utils;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    RoleRepository roleRepository;

    @Override
    public UserDto createUser(UserDto user) {
        ModelMapper modelMapper = new ModelMapper();

        UserEntity checkUserDetails = userRepository.findByEmail(user.getEmail());

        if(checkUserDetails != null) throw new RuntimeException("Record already exists");

        for(int i = 0; i < user.getAddresses().size(); i++) {
            AddressDTO address = user.getAddresses().get(i);
            address.setUserDetails(user);
            address.setAddressId(utils.generateAddressId(30));
            user.getAddresses().set(i, address);
        }

        // for the below operation to work, the fields in UserDto should match with the fields in the UserEntity
        // BeanUtils.copyProperties(user, userEntity);
        UserEntity userEntity = modelMapper.map(user, UserEntity.class);
        String publicUserId = utils.generateUserId(30);

        userEntity.setUserId(publicUserId);
        userEntity.setEncryptedPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        userEntity.setEmailVerificationToken(utils.generateEmailVerificationToken(publicUserId));
        userEntity.setEmailVerificationStatus(false);

        // Set Roles
        Collection<RoleEntity> roleEntities = new HashSet<>();
        for(String role: user.getRoles()) {
            RoleEntity roleEntity = roleRepository.findByName(role);
            if(roleEntity != null) {
                roleEntities.add(roleEntity);
            }
        }

        userEntity.setRoles(roleEntities);

        UserEntity storedUserDetails = userRepository.save(userEntity);
        // BeanUtils.copyProperties(storedUserDetails, returnValue);

        UserDto returnValue = modelMapper.map(storedUserDetails, UserDto.class);

        // Send an email to verify their email address
        new AmazonSES().verifyEmail(returnValue);

        return returnValue;
    }

    @Override
    public UserDto getUser(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);

        if(userEntity == null) throw new UsernameNotFoundException(email);

        UserDto returnValue = new UserDto();
        BeanUtils.copyProperties(userEntity, returnValue);
        return returnValue;
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        UserDto returnValue = new UserDto();
        // the next step is to make use of the userRepository to query our DB for a user that will match the provided userId
        UserEntity userEntity = userRepository.findByUserId(userId);

        if(userEntity == null) throw new UsernameNotFoundException("User with ID: " + userId + " not found");

        BeanUtils.copyProperties(userEntity, returnValue);

        return returnValue;
    }

    @Override
    public UserDto updateUser(String id, UserDto user) {
        UserDto returnValue = new UserDto();
        UserEntity userEntity = userRepository.findByUserId(id);
        if(userEntity == null) throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());

        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());

        UserEntity updatedUserDetails = userRepository.save(userEntity);
        BeanUtils.copyProperties(updatedUserDetails, returnValue);

        return returnValue;
    }

    @Override
    public void deleteUser(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        if(userEntity == null) throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());

        userRepository.delete(userEntity);
    }

    @Override
    public List<UserDto> getUsers(int page, int limit) {
        if(page > 0) page = page - 1;

        List<UserDto> returnValue = new ArrayList<>();
        Pageable pageableRequest = PageRequest.of(page, limit);
        Page<UserEntity> usersPage = userRepository.findAll(pageableRequest);

        List<UserEntity> users = usersPage.getContent();

        for(UserEntity user : users) {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(user, userDto);
            returnValue.add(userDto);
        }

        return returnValue;
    }

    @Override
    public boolean verifyEmailToken(String token) {
        boolean returnValue = false;

        UserEntity userEntity = userRepository.findUserByEmailVerificationToken(token);

        if(userEntity != null) {
            boolean hasTokenExpired = Utils.hasTokenExpired(token);
            if(!hasTokenExpired) {
                userEntity.setEmailVerificationToken(null);
                userEntity.setEmailVerificationStatus(Boolean.TRUE);
                userRepository.save(userEntity);
                returnValue = true;
            }
        }
        return returnValue;
    }

    @Override
    public boolean requestPasswordReset(String email) {
        boolean returnValue = false;
        UserEntity userEntity = userRepository.findByEmail(email);

        if(userEntity == null) {
            return returnValue;
        }

        String token = new Utils().generatePasswordResetToken(userEntity.getUserId());

        PasswordResetTokenEntity passwordResetTokenEntity = new PasswordResetTokenEntity();
        passwordResetTokenEntity.setToken(token);
        passwordResetTokenEntity.setUserDetails(userEntity);
        // because we are using Spring Data JPA, to persist our objects in the DB, we need to use Repository which takes in entity and saves that entity into DB
        passwordResetTokenRepository.save(passwordResetTokenEntity);

        returnValue = new AmazonSES().sendPasswordResetRequest(
                userEntity.getFirstName(),
                userEntity.getEmail(),
                token
        );
        return returnValue;
    }

    @Override
    public boolean resetPassword(String token, String password) {
        boolean returnValue = false;
        if(Utils.hasTokenExpired(token)) {
            return returnValue;
        }

        PasswordResetTokenEntity passwordResetTokenEntity = passwordResetTokenRepository.findByToken(token);
        if(passwordResetTokenEntity == null) {
            return returnValue;
        }
        // prepare new password
        String encodedPassword = bCryptPasswordEncoder.encode(password);
        // Update user password in database
        // it will fetch user details because our database records are linked with @JoinColumn
        // user details will be loaded from the users table
        UserEntity userEntity = passwordResetTokenEntity.getUserDetails();
        userEntity.setEncryptedPassword(encodedPassword);
        UserEntity savedUserEntity = userRepository.save(userEntity);
        // Verify if password was saved successfully
        if(savedUserEntity != null && savedUserEntity.getEncryptedPassword().equalsIgnoreCase(encodedPassword)) {
            returnValue = true;
        }
        // remove password reset token from database
        passwordResetTokenRepository.delete(passwordResetTokenEntity);
        return returnValue;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // This method is being used by spring framework and it will help spring framework to load user details when
        // it needs and this method will be used in the process of user sign in.
        UserEntity userEntity = userRepository.findByEmail(email);

        if(userEntity == null) throw new UsernameNotFoundException(email);

        // return new User(userEntity.getEmail(), userEntity.getEncryptedPassword(), new ArrayList<>());
        //        return new User(
        //            userEntity.getEmail(),
        //            userEntity.getEncryptedPassword(),
        //            userEntity.getEmailVerificationStatus(),
        //            true,
        //            true,
        //            true,
        //            new ArrayList<>()
        //        );
        return new UserPrincipal(userEntity);
    }
}
