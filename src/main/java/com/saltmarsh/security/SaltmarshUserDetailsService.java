package com.saltmarsh.security;

import com.saltmarsh.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaltmarshUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public SaltmarshUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountRepository.findByEmailIgnoreCase(username)
                .map(SaltmarshUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
