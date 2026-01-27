package com.miguel_damasco.DoSafe.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.miguel_damasco.DoSafe.user.domain.UserModel;


@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
    
    UserModel findByUsername(String username);
}
