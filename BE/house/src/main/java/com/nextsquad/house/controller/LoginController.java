package com.nextsquad.house.controller;

import com.nextsquad.house.dto.JwtResponseDto;
import com.nextsquad.house.dto.OauthLoginRequestDto;
import com.nextsquad.house.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/login")
public class LoginController {

    private final LoginService loginService;

    @GetMapping("/oauth/callback")
    public String getOauthAuthCode() {
        return "";
    }

    @PostMapping("/oauth")
    public ResponseEntity<JwtResponseDto> loginWithOauth(@RequestBody OauthLoginRequestDto requestDto) {
        return ResponseEntity.ok(loginService.loginWithOauth(requestDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponseDto> refreshJwtToken(@RequestHeader(value = "access-token") String accessToken, @RequestHeader(value = "refresh-token") String refreshToken) {
        return ResponseEntity.ok(loginService.refreshJwtToken(accessToken, refreshToken));
    }
}
