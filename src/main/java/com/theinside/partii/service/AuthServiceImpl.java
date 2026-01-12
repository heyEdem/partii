package com.theinside.partii.service;

import com.theinside.partii.repository.UserRepository;
import com.theinside.partii.utils.validators.CustomValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final CustomValidator customValidator;
}
