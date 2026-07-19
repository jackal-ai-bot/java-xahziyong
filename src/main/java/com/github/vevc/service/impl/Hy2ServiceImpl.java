package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import com.github.vevc.util.TlsCertGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

/**
 * @author vevc
 */
@Slf4j
@Service
public class Hy2ServiceImpl extends AbstractAppService {

    private static final String APP_NAME = "h2";
    private static final String APP_CONFIG_NAME = "config.yaml";
    private static final String APP_DOWNLOAD_URL = "https://github.com/apernet/hysteria/releases/download/app%%2Fv%s/hysteria-linux-%s";
    private static final String APP_CONFIG_URL = "https://raw.githubusercontent.com/vevc/java-xah/refs/heads/main/hysteria-config.yaml";

    public Hy2ServiceImpl(AppConfig appConfig) {
        super(appConfig);
    }

    @Override
    protected String getAppDownloadUrl() {
        String arch = OS_IS_ARM ? "arm64" : "amd64";
        return String.format(APP_DOWNLOAD_URL, appConfig.getHy2Version(), arch);
    }

    @Override
    public void install() throws Exception {
        // if hy2 exists, skip install
        if (new File(this.getBinaryPath(), APP_NAME).exists()) {
            log.info("Hy2 already exists, skip install");
            return;
        }

        File binaryPath = this.initBinaryPath();
        File destFile = new File(binaryPath, APP_NAME);
        this.download(this.getAppDownloadUrl(), destFile);
        log.info("Hy2 downloaded successfully");
        this.setExecutePermission(destFile.toPath());
        log.info("Hy2 installed successfully");

        // generate tls cert
        TlsCertGenerator.generate(appConfig.getDomain(), 3650, 2048, binaryPath);
        log.info("Hy2 TLS cert generated successfully");

        // download config
        this.downloadConfig(binaryPath);
        log.info("Hy2 config downloaded successfully");
    }

    private void downloadConfig(File configPath) throws Exception {
        String content;
        try (InputStream in = new URL(APP_CONFIG_URL).openStream()) {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        String configText = content.replace("10008", appConfig.getPort())
                .replace("CERT_FILE_PATH", configPath.getAbsolutePath())
                .replace("HY2_PASSWORD", appConfig.getUuid());
        File configFile = new File(configPath, APP_CONFIG_NAME);
        Files.writeString(configFile.toPath(), configText,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Async
    @Override
    public void startup() throws Exception {
        File binaryPath = this.getBinaryPath();
        File appFile = new File(binaryPath, APP_NAME);
        File configFile = new File(binaryPath, APP_CONFIG_NAME);
        while (true) {
            ProcessBuilder pb = new ProcessBuilder(appFile.getAbsolutePath(),
                    "server", "-c", configFile.getAbsolutePath());
            pb.redirectOutput(new File("/dev/null"));
            pb.redirectError(new File("/dev/null"));
            log.info("Starting Hy2...");
            int exitCode = this.startProcess(pb);
            if (exitCode == 0) {
                log.info("Hy2 process exited with code: {}", exitCode);
                break;
            } else {
                log.info("Hy2 process exited with code: {}, restarting...", exitCode);
                TimeUnit.SECONDS.sleep(3);
            }
        }
    }

    @Override
    protected String getAppName() {
        return APP_NAME;
    }
}
