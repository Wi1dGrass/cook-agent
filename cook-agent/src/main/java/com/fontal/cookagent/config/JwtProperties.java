package com.fontal.cookagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "cook.security.jwt")
public class JwtProperties {

    /** JWT 签名密钥（Base64） */
    private String secret;

    /** 访问令牌 TTL */
    private Duration accessTokenTtl = Duration.ofMinutes(120);

    /** 签发者 */
    private String issuer = "cook-agent";
}