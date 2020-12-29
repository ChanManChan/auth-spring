package com.chan.ws.mobileappws.io.repositories;

import com.chan.ws.mobileappws.io.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends PagingAndSortingRepository<UserEntity, Long> {
        // Spring Data JPA simplifies the DB communication very much, all we need to do is to remember how to properly name these methods
        // Query methods (the way you are going to name this method, Spring Data JPA is going to compose a SQL query and execute that query against the DB)
        UserEntity findByEmail(String email);
        UserEntity findByUserId(String userId);
        UserEntity findUserByEmailVerificationToken(String token);
}
