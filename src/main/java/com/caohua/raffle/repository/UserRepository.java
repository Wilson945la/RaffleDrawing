package com.caohua.raffle.repository;

import com.caohua.raffle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAccountId(String accountId);
    boolean existsByAccountId(String accountId);
    boolean existsByRealName(String realName);

    List<User> findByAccountIdContainingOrRealNameContainingIgnoreCase(String accountId, String realName);
}
