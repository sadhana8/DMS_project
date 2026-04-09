package com.dms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the <b>DocVault Document Management System</b>.
 *
 * <p>
 * This class bootstraps the Spring Boot application and enables:
 * <ul>
 * <li>{@code @EnableAsync} – allows service methods (e.g. email sending) to run
 * on a background thread pool so they never block HTTP responses.</li>
 * <li>{@code @EnableScheduling} – activates {@code @Scheduled} tasks such as
 * expired-token cleanup jobs.</li>
 * </ul>
 *
 * <p>
 * <b>Default startup sequence:</b>
 * <ol>
 * <li>Connects to PostgreSQL via the datasource in
 * {@code application.properties}.</li>
 * <li>{@link com.dms.config.DataInitializer} seeds all four roles and the
 * default admin account ({@code admin@dms.com / Admin@123}) if they do not
 * already exist.</li>
 * <li>Spring Security wires the JWT filter chain via
 * {@link com.dms.config.SecurityConfig}.</li>
 * <li>Embedded Tomcat starts on port {@code 8080} with context path
 * {@code /api}.</li>
 * </ol>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DmsApplication {

    /**
     * Application entry point.
     *
     * @param args optional command-line arguments forwarded to Spring Boot
     * (e.g. {@code --server.port=9090} or
     * {@code --spring.profiles.active=prod})
     */
    public static void main(String[] args) {
        SpringApplication.run(DmsApplication.class, args);
    }
}
