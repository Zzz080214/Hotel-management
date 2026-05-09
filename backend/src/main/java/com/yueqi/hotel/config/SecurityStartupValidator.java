package com.yueqi.hotel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SecurityStartupValidator implements ApplicationRunner {

    private static final String DEFAULT_TOKEN_SECRET = "local-dev-token-secret-change-before-production";

    @Value("${app.production:false}")
    private boolean production;

    @Value("${auth.token.secret:" + DEFAULT_TOKEN_SECRET + "}")
    private String tokenSecret;

    @Value("${wx.mini-program.dev-login-enabled:true}")
    private boolean wxDevLoginEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!production) {
            return;
        }
        if (!StringUtils.hasText(tokenSecret) || DEFAULT_TOKEN_SECRET.equals(tokenSecret)) {
            throw new IllegalStateException("生产环境必须配置 AUTH_TOKEN_SECRET");
        }
        if (tokenSecret.length() < 32) {
            throw new IllegalStateException("AUTH_TOKEN_SECRET 长度不能少于 32 位");
        }
        if (wxDevLoginEnabled) {
            throw new IllegalStateException("生产环境必须关闭 WX_DEV_LOGIN_ENABLED");
        }
    }
}
