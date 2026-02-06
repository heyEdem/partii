package com.theinside.partii.service;

import com.theinside.partii.dto.CompleteProfileRequest;
import com.theinside.partii.dto.UpdateProfileRequest;
import com.theinside.partii.dto.UserProfileResponse;
import com.theinside.partii.entity.User;
import com.theinside.partii.exception.BadRequestException;
import com.theinside.partii.exception.NotFoundException;
import com.theinside.partii.mapper.UserMapper;
import com.theinside.partii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.theinside.partii.utils.CustomMessages.USER_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        userMapper.updateUserFromRequest(request, user);

        User savedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", savedUser.getEmail());

        return userMapper.toProfileResponse(savedUser);
    }


    @Override
    @Transactional
    public UserProfileResponse completeProfile(Long userId, CompleteProfileRequest request) {
        User user = findUserById(userId);

        if (user.isProfileCompleted()) {
            throw new BadRequestException("Profile already completed");
        }

        if (user.getDob() != null) {
            throw new BadRequestException("Date of birth already set");
        }

        userMapper.completeProfileFromRequest(request, user);
        user.setProfileCompleted(true);

        User savedUser = userRepository.save(user);
        log.info("Profile completed for user: {}", savedUser.getEmail());

        return userMapper.toProfileResponse(savedUser);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
    }
}
