package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author vevc
 */
@Slf4j
@Service
public class XrayServiceImpl extends AbstractAppService {

    private static final String APP_NAME = "xy";
    private static final String APP_CONFIG_NAME = "config.json";
    private static final String APP_ARCHIVE_NAME = "Xray-linux.zip";
    private static final String APP_DOWNLOAD_URL = "https://github.com/XTLS/Xray-core/releases/download/v%s/Xray-linux-%s.zip";
    private static final String APP_CONFIG_URL = "https://raw.githubusercontent.com/vevc/java-xah/refs/heads/main/xray-config.json";

    private static final String REALITY_PRIVATE_KEY_PREFIX = "PrivateKey: ";
    private static final String REALITY_PUBLIC_KEY_PREFIX = "Password: ";

    public XrayServiceImpl(AppConfig appConfig) {
        super(appConfig);
    }

    @Override
    protected String getAppDownloadUrl() {
        String arch = OS_IS_ARM ? "arm64-v8a" : "64";
        return String.format(APP_DOWNLOAD_URL, appConfig.getXrayVersion(), arch);
    }

    @Override
    public void install() throws Exception {
        // if xray exists, skip install
        if (new File(this.getBinaryPath(), APP_NAME).exists()) {
            log.info("Xray already exists, skip install");
            return;
        }

        File binaryPath = this.initBinaryPath();
        File targetFile = new File(binaryPath, APP_ARCHIVE_NAME);
        this.download(this.getAppDownloadUrl(), targetFile);
        log.info("Xray archive downloaded successfully");
        try (ZipFile zipFile = new ZipFile(targetFile)) {
            zipFile.extractAll(binaryPath.getAbsolutePath());
        }
        log.info("Xray archive extracted successfully");
        // delete zip file
        FileUtils.delete(targetFile);
        // rename
        File srcFile = new File(binaryPath, "xray");
        File destFile = new File(binaryPath, APP_NAME);
        FileUtils.moveFile(srcFile, destFile);
        this.setExecutePermission(destFile.toPath());
        log.info("Xray installed successfully");

        // download config
        this.downloadConfig(binaryPath);
        log.info("Xray config downloaded successfully");

        // update application.yml config
        this.updateSpringConfig();
        log.info("Spring application.yml config updated successfully");
    }

    private void downloadConfig(File binaryPath) throws Exception {
        String content;
        try (InputStream in = new URL(APP_CONFIG_URL).openStream()) {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        this.generateRealityKeys(new File(binaryPath, APP_NAME));
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        appConfig.setRealityShortId(shortId);

        String configText = content.replace("10008", appConfig.getPort())
                .replace("YOUR_UUID", appConfig.getUuid())
                .replace("YOUR_PRIVATE_KEY", appConfig.getRealityPrivateKey())
                .replace("YOUR_SHORT_ID", shortId);
        File configFile = new File(binaryPath, APP_CONFIG_NAME);
        Files.writeString(configFile.toPath(), configText,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void generateRealityKeys(File binaryFile) throws Exception {
        String[] command = {binaryFile.getAbsolutePath(), "x25519"};
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (
                InputStream in = process.getInputStream();
                InputStreamReader inReader = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(inReader)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(REALITY_PRIVATE_KEY_PREFIX)) {
                    String privateKey = line.replace(REALITY_PRIVATE_KEY_PREFIX, StringUtils.EMPTY);
                    appConfig.setRealityPrivateKey(privateKey);
                } else if (line.startsWith(REALITY_PUBLIC_KEY_PREFIX)) {
                    String publicKey = line.replace(REALITY_PUBLIC_KEY_PREFIX, StringUtils.EMPTY);
                    appConfig.setRealityPublicKey(publicKey);
                }
            }
        }
        int exitCode = process.waitFor();
        Assert.isTrue(exitCode == 0, "Failed to generate reality keys");
    }

    @Async
    @Override
    public void startup() throws Exception {
        File binaryPath = this.getBinaryPath();
        File appFile = new File(binaryPath, APP_NAME);
        File configFile = new File(binaryPath, APP_CONFIG_NAME);
        while (true) {
            ProcessBuilder pb = new ProcessBuilder(appFile.getAbsolutePath(),
                    "-c", configFile.getAbsolutePath());
            pb.redirectOutput(new File("/dev/null"));
            pb.redirectError(new File("/dev/null"));
            log.info("Starting Xray...");
            int exitCode = this.startProcess(pb);
            if (exitCode == 0) {
                log.info("Xray process exited with code: {}", exitCode);
                break;
            } else {
                log.info("Xray process exited with code: {}, restarting...", exitCode);
                TimeUnit.SECONDS.sleep(3);
            }
        }
    }

    @Override
    protected String getAppName() {
        return APP_NAME;
    }
}
