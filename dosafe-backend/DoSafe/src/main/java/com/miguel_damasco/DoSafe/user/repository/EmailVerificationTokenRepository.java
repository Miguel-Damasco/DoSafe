package com.miguel_damasco.DoSafe.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.miguel_damasco.DoSafe.user.domain.EmailVerificationToken;
import com.miguel_damasco.DoSafe.user.domain.UserModel;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String pToken);

    // Used before resending — fetches all unused tokens for a user so they can be invalidated.
    List<EmailVerificationToken> findAllByUserAndUsedAtIsNull(UserModel pUser);
}
