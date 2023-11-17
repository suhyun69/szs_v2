package com.szs.v2.controllers;

import com.szs.v2.configs.jwt.TokenProvider;
import com.szs.v2.domain.User;
import com.szs.v2.dto.AddUserRequest;
import com.szs.v2.dto.SignInRequest;
import com.szs.v2.dto.SignInResponse;
import com.szs.v2.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user/api")
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@RequestBody AddUserRequest request) {

        Long userId = userService.save(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userId);
    }

    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signin(@RequestBody SignInRequest request) {

        try {
            // username, password 인증 시도
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("로그인 실패");
        }

        User user = userService.findByEmail(request.getEmail());
        String token = tokenProvider.generateToken(user, Duration.ofDays(1));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignInResponse(token));
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());

        return ResponseEntity.ok()
                .build();
    }
}
