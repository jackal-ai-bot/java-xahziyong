package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vevc
 */
@Slf4j
@Service
public class ArgoServiceImpl extends AbstractAppService {

    private static final String APP_NAME = "cf";
    private static final String APP_DOWNLOAD_URL = "https://github.com/cloudflare/cloudflared/releases/download/%s/cloudflared-linux-%s";
    private static final Pattern QUICK_TUNNEL_HOST_PATTERN = Pattern.compile("https://[a-z0-9-]+\\.trycloudflare\\.com");
    private static final String WS_URL = "vless://%s@%s:443?encryption=none&security=tls&sni=%s&fp=chrome&type=ws&path=%%2F%%3Fed%%3D2560#%s-ws-argo";
    private static final String REALITY_URL = "vless://%s@%s:%s?encryption=none&flow=xtls-rprx-vision&security=reality&sni=www.cloudflare.com&fp=chrome&pbk=%s&sid=%s&spx=%%2F&type=tcp&headerType=none#%s-reality";
    private static final String HY2_URL = "hysteria2://%s@%s:%s?insecure=1#%s-hy2";
    private static final Path NODE_FILE_PATH = Paths.get(System.getProperty("user.dir"), "node.txt");

    public ArgoServiceImpl(AppConfig appConfig) {
        super(appConfig);
    }

    @Override
    protected String getAppDownloadUrl() {
        String arch = OS_IS_ARM ? "arm64" : "amd64";
        return String.format(APP_DOWNLOAD_URL, appConfig.getArgoVersion(), arch);
    }

    @Override
    public void install() throws Exception {
        // if argo exists, skip install
        if (new File(this.getBinaryPath(), APP_NAME).exists()) {
            log.info("Argo already exists, skip install");
            return;
        }

        File binaryPath = this.initBinaryPath();
        File destFile = new File(binaryPath, APP_NAME);
        this.download(this.getAppDownloadUrl(), destFile);
        log.info("Argo downloaded successfully");
        this.setExecutePermission(destFile.toPath());
        log.info("Argo installed successfully");
        this.updateSubFile();
    }

    @Async
    @Override
    public void startup() throws Exception {
        File appFile = new File(this.getBinaryPath(), APP_NAME);
        Process process;
        while (true) {
            if (StringUtils.isBlank(appConfig.getArgoToken())) {
                log.info("Starting Argo...");
                ProcessBuilder pb = new ProcessBuilder(appFile.getAbsolutePath(), "tunnel", "--no-autoupdate",
                        "--edge-ip-version", "auto", "--protocol", "http2", "--url", "http://localhost:8001");
                pb.redirectErrorStream(true);
                process = pb.start();
                AtomicBoolean stopLogging = new AtomicBoolean(false);
                try (
                        InputStream is = process.getInputStream();
                        InputStreamReader isReader = new InputStreamReader(is);
                        BufferedReader reader = new BufferedReader(isReader)
                ) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (stopLogging.get()) {
                            continue;
                        }
                        Matcher matcher = QUICK_TUNNEL_HOST_PATTERN.matcher(line);
                        String lastMatch = null;
                        while (matcher.find()) {
                            lastMatch = matcher.group();
                        }
                        if (lastMatch != null) {
                            stopLogging.set(true);
                            String argoDomain = new URL(lastMatch).getHost();
                            appConfig.setArgoDomain(argoDomain);
                            // update application.yml config
                            updateSpringConfig();
                            log.info("Spring application.yml config updated successfully");
                            updateSubFile();
                            log.info("✅ Startup completed. You can view node details at: {}", NODE_FILE_PATH);
                        }
                    }
                }
            } else {
                updateSubFile();
                log.info("✅ Startup completed. You can view node details at: {}", NODE_FILE_PATH);
                ProcessBuilder pb = new ProcessBuilder(appFile.getAbsolutePath(), "tunnel", "--no-autoupdate",
                        "--edge-ip-version", "auto", "--protocol", "http2", "run", "--token", appConfig.getArgoToken());
                pb.redirectOutput(new File("/dev/null"));
                pb.redirectError(new File("/dev/null"));
                log.info("Starting Argo...");
                process = pb.start();
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Argo process exited with code: {}", exitCode);
                break;
            } else {
                log.info("Argo process exited with code: {}, restarting...", exitCode);
                TimeUnit.SECONDS.sleep(3);
            }
        }
    }

    private void updateSubFile() throws IOException {
        List<String> subInfoList = new ArrayList<>();
        String wsUrl = String.format(WS_URL, appConfig.getUuid(),
                appConfig.getArgoDomain(), appConfig.getArgoDomain(), appConfig.getRemarksPrefix());
        subInfoList.add(wsUrl);
        String realityUrl = String.format(REALITY_URL, appConfig.getUuid(), appConfig.getDomain(), appConfig.getPort(),
                appConfig.getRealityPublicKey(), appConfig.getRealityShortId(), appConfig.getRemarksPrefix());
        subInfoList.add(realityUrl);
        String hy2Url = String.format(HY2_URL, appConfig.getUuid(), appConfig.getDomain(), appConfig.getPort(),
                appConfig.getRemarksPrefix());
        subInfoList.add(hy2Url);
        Files.write(NODE_FILE_PATH, subInfoList);
    }

    @Override
    protected String getAppName() {
        return APP_NAME;
    }
}
