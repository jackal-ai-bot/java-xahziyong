package com.github.vevc;

import com.github.vevc.service.AppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author vevc
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppRunner implements CommandLineRunner {

    private final AppService appService;

    @Override
    public void run(String... args) throws Exception {
        appService.install();
        appService.startup();
        ProcessBuilder pb = new ProcessBuilder("bash");
        pb.inheritIO();
        log.info("Starting bash...");
        Process process = pb.start();
        int exitCode = process.waitFor();
        log.info("Bash exited with code: " + exitCode);
    }
}
