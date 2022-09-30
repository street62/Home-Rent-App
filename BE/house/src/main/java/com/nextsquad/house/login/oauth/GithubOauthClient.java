package com.nextsquad.house.login.oauth;

import com.google.gson.Gson;
import com.nextsquad.house.dto.login.GithubAccessTokenResponseDto;
import com.nextsquad.house.login.userinfo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Map;

@Slf4j
public class GithubOauthClient extends OauthClient {
    public GithubOauthClient(String clientId, String authServerUrl, String resourceServerUrl, String secretKey) {
        super(clientId, authServerUrl, resourceServerUrl, secretKey);
    }

    @Override
    protected String getAccessToken(String authCode) {
        GithubAccessTokenResponseDto rawToken = webClient.post()
                .uri(authServerUrl, uriBuilder -> uriBuilder
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", secretKey)
                        .queryParam("code", authCode)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .ifNoneMatch("*")
                .ifModifiedSince(ZonedDateTime.now())
                .retrieve()
                .bodyToFlux(GithubAccessTokenResponseDto.class)
                .toStream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException());
        return parseToken(rawToken.getAccessToken());
    }

    @Override
    protected UserInfo getOauthUserInfo(String accessToken) {
        String response = webClient.get()
                .uri(resourceServerUrl)
                .header("authorization", accessToken)
                .acceptCharset(StandardCharsets.UTF_8)
                .retrieve()
                .bodyToFlux(String.class)
                .toStream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException());
        return convertToUserInfoFrom(response);
    }

    @Override
    protected String parseToken(String rawToken) {
        return String.format("token %s", rawToken);
    }

    protected UserInfo convertToUserInfoFrom(String rawInfo) {
        Map<String, String> infoMap = new Gson().fromJson(rawInfo, Map.class);
        return new UserInfo(infoMap.get("login"), infoMap.get("login"), null, OauthClientType.GITHUB);
    }
}

