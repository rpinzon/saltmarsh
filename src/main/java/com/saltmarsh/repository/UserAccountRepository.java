package com.saltmarsh.repository;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<UserAccount> findByRoleInAndEnabledTrueOrderByFullNameAsc(List<Role> roles);
}
