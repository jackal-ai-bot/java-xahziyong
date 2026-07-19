package com.github.vevc.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * @author vevc
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String domain;
    private String port;
    private String uuid;
    private String xrayVersion;
    private String hy2Version;
    private String argoVersion;
    private String argoDomain;
    private String argoToken;
    private String realityPublicKey;
    private String realityPrivateKey;
    private String realityShortId;
    private String remarksPrefix;
    private String nezhaVersion;
    private String nezhaServer;
    private String nezhaClientSecret;
    private String nezhaTls;

    @PostConstruct
    public void init() {
        domain = StringUtils.defaultIfBlank(domain, "vevc.github.com");
        port = StringUtils.defaultIfBlank(port, "10008");
        uuid = StringUtils.defaultIfBlank(uuid, UUID.randomUUID().toString());
        xrayVersion = StringUtils.defaultIfBlank(xrayVersion, "25.10.15");
        hy2Version = StringUtils.defaultIfBlank(hy2Version, "2.6.5");
        argoVersion = StringUtils.defaultIfBlank(argoVersion, "2025.10.0");
        argoDomain = StringUtils.defaultIfBlank(argoDomain, "xxx.trycloudflare.com");
        remarksPrefix = StringUtils.defaultIfBlank(remarksPrefix, "vevc");
        nezhaTls = StringUtils.defaultIfBlank(nezhaTls, "false");
    }
}
