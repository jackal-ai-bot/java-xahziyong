package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

/**
 * Nezha monitoring agent (v1, https://github.com/nezhahq/agent) integration.
 * This service is optional: it only installs/starts when {@code nezha-server}
 * and {@code nezha-client-secret} are configured in application.yml.
 *
 * @author vevc
 */
@Slf4j
@Service
public class NezhaServiceImpl extends AbstractAppService {

    private static final String APP_NAME = "nz";
    private static final String APP_CONFIG_NAME = "config.yml";
    private static final String APP_ARCHIVE_NAME = "nezha-agent.zip";
    private static final String APP_DOWNLOAD_URL = "https://github.com/nezhahq/agent/releases/download/%s/nezha-agent_linux_%s.zip";
    private static final String APP_LATEST_DOWNLOAD_URL = "https://github.com/nezhahq/agent/releases/latest/download/nezha-agent_linux_%s.zip";

    public NezhaServiceImpl(AppConfig appConfig) {
        super(appConfig);
    }

    private boolean isEnabled() {
        return StringUtils.isNoneBlank(appConfig.getNezhaServer(), appConfig.getNezhaClientSecret());
    }

    @Override
    protected String getAppDownloadUrl() {
        String arch = OS_IS_ARM ? "arm64" : "amd64";
        if (StringUtils.isBlank(appConfig.getNezhaVersion())) {
            return String.format(APP_LATEST_DOWNLOAD_URL, arch);
        }
        return String.format(APP_DOWNLOAD_URL, appConfig.getNezhaVersion(), arch);
    }

    @Override
    public void install() throws Exception {
        if (!this.isEnabled()) {
            log.info("Nezha agent is not configured, skip install");
            return;
        }

        // if nezha agent exists, skip install
        if (new File(this.getBinaryPath(), APP_NAME).exists()) {
            log.info("Nezha agent already exists, skip install");
            return;
        }

        File binaryPath = this.initBinaryPath();
        File targetFile = new File(binaryPath, APP_ARCHIVE_NAME);
        this.download(this.getAppDownloadUrl(), targetFile);
        log.info("Nezha agent archive downloaded successfully");
        try (ZipFile zipFile = new ZipFile(targetFile)) {
            zipFile.extractAll(binaryPath.getAbsolutePath());
        }
        log.info("Nezha agent archive extracted successfully");
        // delete zip file
        FileUtils.delete(targetFile);
        // rename
        File srcFile = new File(binaryPath, "nezha-agent");
        File destFile = new File(binaryPath, APP_NAME);
        FileUtils.moveFile(srcFile, destFile);
        this.setExecutePermission(destFile.toPath());
        log.info("Nezha agent installed successfully");

        // generate config
        this.generateConfig(binaryPath);
        log.info("Nezha agent config generated successfully");
    }

    private void generateConfig(File binaryPath) throws Exception {
        String configText = "client_secret: " + appConfig.getNezhaClientSecret() + "\n" +
                "debug: false\n" +
                "disable_auto_update: true\n" +
                "disable_command_execute: true\n" +
                "disable_force_update: true\n" +
                "disable_nat: false\n" +
                "disable_send_query: false\n" +
                "gpu: false\n" +
                "insecure_tls: false\n" +
                "ip_report_period: 1800\n" +
                "report_delay: 3\n" +
                "server: " + appConfig.getNezhaServer() + "\n" +
                "skip_connection_count: false\n" +
                "skip_procs_count: false\n" +
                "temperature: false\n" +
                "tls: " + appConfig.getNezhaTls() + "\n" +
                "uuid: " + appConfig.getUuid() + "\n";
        File configFile = new File(binaryPath, APP_CONFIG_NAME);
        Files.writeString(configFile.toPath(), configText,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Async
    @Override
    public void startup() throws Exception {
        if (!this.isEnabled()) {
            return;
        }

        File binaryPath = this.getBinaryPath();
        File appFile = new File(binaryPath, APP_NAME);
        File configFile = new File(binaryPath, APP_CONFIG_NAME);
        while (true) {
            ProcessBuilder pb = new ProcessBuilder(appFile.getAbsolutePath(),
                    "-c", configFile.getAbsolutePath());
            pb.redirectOutput(new File("/dev/null"));
            pb.redirectError(new File("/dev/null"));
            log.info("Starting Nezha agent...");
            int exitCode = this.startProcess(pb);
            if (exitCode == 0) {
                log.info("Nezha agent process exited with code: {}", exitCode);
                break;
            } else {
                log.info("Nezha agent process exited with code: {}, restarting...", exitCode);
                TimeUnit.SECONDS.sleep(3);
            }
        }
    }

    @Override
    protected String getAppName() {
        return APP_NAME;
    }
}
