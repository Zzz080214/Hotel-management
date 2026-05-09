package com.yueqi.hotel.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 确保所有 JSON 响应 Content-Type 携带 charset=UTF-8，避免微信小程序端中文乱码。
        // Spring 6.x 的 application/json 默认不带 charset，部分微信客户端可能无法正确解码中文。
        converters.stream()
                .filter(c -> c instanceof MappingJackson2HttpMessageConverter)
                .map(c -> (MappingJackson2HttpMessageConverter) c)
                .forEach(c -> {
                    c.setDefaultCharset(StandardCharsets.UTF_8);
                    List<MediaType> types = c.getSupportedMediaTypes().stream()
                            .map(mt -> new MediaType(mt, StandardCharsets.UTF_8))
                            .toList();
                    c.setSupportedMediaTypes(types);
                });
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        "/api/auth/**"   // 登录/登出接口不拦截
                );
    }
}
