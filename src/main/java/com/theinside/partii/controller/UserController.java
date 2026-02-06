package com.theinside.partii.controller;

import com.theinside.partii.dto.CompleteProfileRequest;
import com.theinside.partii.dto.GenericMessageResponse;
import com.theinside.partii.dto.UpdateProfileRequest;
import com.theinside.partii.dto.UserProfileResponse;
import com.theinside.partii.entity.User;
import com.theinside.partii.security.SecurityUser;
import com.theinside.partii.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/partii/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal SecurityUser user) {
        return userService.getProfile(user.getUserId());
    }

    @PatchMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileResponse updateMyProfile(@AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user.getUserId(), request);
    }
}
