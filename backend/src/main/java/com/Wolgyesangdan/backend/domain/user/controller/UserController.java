package com.Wolgyesangdan.backend.domain.user.controller;

import com.Wolgyesangdan.backend.domain.user.dto.ContactResponse;
import com.Wolgyesangdan.backend.domain.user.dto.ContactUpdateRequest;
import com.Wolgyesangdan.backend.domain.user.dto.MyInfoResponse;
import com.Wolgyesangdan.backend.domain.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public MyInfoResponse getMyInfo(@AuthenticationPrincipal Long userId) {
		return userService.getMyInfo(userId);
	}

	@GetMapping("/me/contact")
	public ContactResponse getMyContact(@AuthenticationPrincipal Long userId) {
		return userService.getMyContact(userId);
	}

	@PutMapping("/me/contact")
	public ContactResponse updateMyContact(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody ContactUpdateRequest request) {
		return userService.updateMyContact(userId, request);
	}

}
