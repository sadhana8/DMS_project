package com.dms.config;

import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * General application-level Spring beans that do not belong to a specific
 * domain.
 *
 * <p>
 * This configuration class provides two shared beans:
 * <ul>
 * <li>An Apache <b>Tika</b> instance for MIME-type detection of uploaded
 * files.</li>
 * <li>A bounded <b>thread-pool executor</b> used by all {@code @Async} methods
 * (primarily the {@link com.dms.service.EmailService}).</li>
 * </ul>
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class AppConfig {

    /**
     * Provides a shared, thread-safe {@link Tika} instance for MIME-type
     * detection.
     *
     * <p>
     * Apache Tika inspects the actual byte content of a file (magic bytes),
     * which is far more reliable than trusting the {@code Content-Type} header
     * or the file extension supplied by the client.
     *
     * <p>
     * Usage example inside a service:
     * <pre>{@code
     * String mimeType = tika.detect(multipartFile.getInputStream());
     * }</pre>
     *
     * @return a singleton {@link Tika} instance
     */
    @Bean
    public Tika tika() {
        return new Tika();
    }

    /**
     * Configures the thread-pool executor used by Spring's {@code @Async}
     * infrastructure.
     *
     * <p>
     * Parameters chosen for a typical development / small-production workload:
     * <ul>
     * <li><b>Core pool size 4</b> – always-alive threads for low-load
     * periods.</li>
     * <li><b>Max pool size 16</b> – ceiling during traffic spikes.</li>
     * <li><b>Queue capacity 100</b>– tasks queued before new threads are
     * spawned.</li>
     * </ul>
     *
     * <p>
     * All async tasks (welcome emails, password-reset emails, share
     * notifications) execute on threads named {@code dms-async-N}, making them
     * easy to identify in logs and thread dumps.
     *
     * @return a fully initialised {@link Executor} registered under the bean
     * name {@code "taskExecutor"}
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("dms-async-");
        executor.initialize();
        return executor;
    }
}
