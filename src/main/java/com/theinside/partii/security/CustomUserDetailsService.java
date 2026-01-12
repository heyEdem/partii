package com.theinside.partii.security;

import com.theinside.partii.entity.User;
import com.theinside.partii.utils.validators.CustomValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final CustomValidator customValidator;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User account = customValidator.loadAccountByEmail(email);

        return new SecurityUser(account);
    }
}
