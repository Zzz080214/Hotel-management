package com.yueqi.hotel.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.yueqi.hotel.dto.WxLoginRequest;
import com.yueqi.hotel.dto.WxLoginResponse;

@Service
public class WxAuthService {

    public record WxSession(String openid, String token, String nickname, String avatarUrl) {}

    private final RestTemplate restTemplate = new RestTemplate();
    private final TokenService tokenService;

    public WxAuthService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Value("${wx.mini-program.appid:}")
    private String appId;

    @Value("${wx.mini-program.secret:}")
    private String secret;

    @Value("${wx.mini-program.dev-login-enabled:true}")
    private boolean devLoginEnabled;

    @Value("${auth.token.wx-ttl-minutes:43200}")
    private long wxTokenTtlMinutes;

    public WxLoginResponse login(WxLoginRequest request) {
        String openid = exchangeOpenid(request);
        String nickname = StringUtils.hasText(request.nickname()) ? request.nickname().trim() : "微信用户";
        String avatarUrl = request.avatarUrl() == null ? "" : request.avatarUrl();
        String token = tokenService.issue(
                "wx",
                openid,
                Duration.ofMinutes(wxTokenTtlMinutes),
                Map.of("nickname", nickname, "avatarUrl", avatarUrl));
        return new WxLoginResponse(token, openid, nickname, avatarUrl);
    }

    public Optional<WxSession> validateToken(String token) {
        return tokenService.verify(token, "wx")
                .map(payload -> new WxSession(
                        payload.subject(),
                        token,
                        payload.claims().getOrDefault("nickname", "微信用户"),
                        payload.claims().getOrDefault("avatarUrl", "")));
    }

    public void logout(String token) {
        tokenService.revoke(token);
    }

    private String exchangeOpenid(WxLoginRequest request) {
        if (StringUtils.hasText(appId) && StringUtils.hasText(secret)) {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                    .queryParam("appid", appId)
                    .queryParam("secret", secret)
                    .queryParam("js_code", request.code())
                    .queryParam("grant_type", "authorization_code")
                    .build(true)
                    .toUri();
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(uri, Map.class);
            if (body == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "微信登录失败");
            }
            Object errCode = body.get("errcode");
            if (errCode != null && Integer.parseInt(String.valueOf(errCode)) != 0) {
                String errMsg = String.valueOf(body.getOrDefault("errmsg", "微信登录失败"));
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, errMsg);
            }
            String openid = (String) body.get("openid");
            if (!StringUtils.hasText(openid)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "微信登录失败，未返回 openid");
            }
            return openid;
        }

        if (!devLoginEnabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "微信登录未配置，请先填写 appid/secret");
        }
        if (StringUtils.hasText(request.devOpenid())) {
            return request.devOpenid().trim();
        }
        return "dev-" + UUID.nameUUIDFromBytes(request.code().getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }
}
