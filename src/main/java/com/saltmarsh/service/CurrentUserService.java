package com.saltmarsh.service;

import com.saltmarsh.domain.UserAccount;
import com.saltmarsh.exception.ForbiddenException;
import com.saltmarsh.exception.NotFoundException;
import com.saltmarsh.repository.UserAccountRepository;
import com.saltmarsh.security.SaltmarshUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserAccountRepository userAccountRepository;

    public CurrentUserService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public UserAccount requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof SaltmarshUserDetails details)) {
            throw new ForbiddenException("Authentication required");
        }
        return userAccountRepository.findById(details.getId())
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }
}
