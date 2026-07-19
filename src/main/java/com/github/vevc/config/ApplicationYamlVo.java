package com.github.vevc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author vevc
 */
@Data
public class ApplicationYamlVo {

    private SpringConfig spring = new SpringConfig();
    private AppConfigVo app = new AppConfigVo();

    public void setAppConfig(AppConfig appConfig) {
        this.getApp().setDomain(appConfig.getDomain());
        this.getApp().setPort(appConfig.getPort());
        this.getApp().setUuid(appConfig.getUuid());
        this.getApp().setXrayVersion(appConfig.getXrayVersion());
        this.getApp().setHy2Version(appConfig.getHy2Version());
        this.getApp().setArgoVersion(appConfig.getArgoVersion());
        this.getApp().setArgoDomain(appConfig.getArgoDomain());
        this.getApp().setArgoToken(appConfig.getArgoToken());
        this.getApp().setRealityPublicKey(appConfig.getRealityPublicKey());
        this.getApp().setRealityPrivateKey(appConfig.getRealityPrivateKey());
        this.getApp().setRealityShortId(appConfig.getRealityShortId());
        this.getApp().setRemarksPrefix(appConfig.getRemarksPrefix());
    }

    @Data
    public static class AppConfigVo {
        private String domain;
        private String port;
        private String uuid;
        @JsonProperty("xray-version")
        private String xrayVersion;
        @JsonProperty("hy2-version")
        private String hy2Version;
        @JsonProperty("argo-version")
        private String argoVersion;
        @JsonProperty("argo-domain")
        private String argoDomain;
        @JsonProperty("argo-token")
        private String argoToken;
        @JsonProperty("reality-public-key")
        private String realityPublicKey;
        @JsonProperty("reality-private-key")
        private String realityPrivateKey;
        @JsonProperty("reality-short-id")
        private String realityShortId;
        @JsonProperty("remarks-prefix")
        private String remarksPrefix;
    }

    @Data
    public static class SpringConfig {
        private Application application = new Application();
    }

    @Data
    public static class Application {
        private String name = "java-xah";
    }
}
