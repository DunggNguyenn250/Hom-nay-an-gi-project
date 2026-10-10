package org.example.homnayangi.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class SwaggerBrowserOpener {

    @Value("${app.swagger.auto-open:true}")
    private boolean autoOpen;

    @Value("${server.port:8080}")
    private int port;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerPath;

    @EventListener(ApplicationReadyEvent.class)
    public void openSwaggerUI() {
        if (!autoOpen) {
            return;
        }

        String cleanContextPath = (contextPath == null || contextPath.isBlank() || "/".equals(contextPath.trim()))
                ? ""
                : contextPath.trim();
        if (!cleanContextPath.isEmpty() && !cleanContextPath.startsWith("/")) {
            cleanContextPath = "/" + cleanContextPath;
        }
        if (cleanContextPath.endsWith("/")) {
            cleanContextPath = cleanContextPath.substring(0, cleanContextPath.length() - 1);
        }

        String cleanSwaggerPath = (swaggerPath == null || swaggerPath.isBlank())
                ? "/swagger-ui.html"
                : swaggerPath.trim();
        if (!cleanSwaggerPath.startsWith("/")) {
            cleanSwaggerPath = "/" + cleanSwaggerPath;
        }

        String url = "http://localhost:" + port + cleanContextPath + cleanSwaggerPath;
        log.info("Đang tự động mở Swagger UI trên trình duyệt: {}", url);

        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else if (os.contains("nix") || os.contains("nux")) {
                new ProcessBuilder("xdg-open", url).start();
            } else {
                log.warn("Hệ điều hành không được hỗ trợ để tự động mở trình duyệt: {}", os);
            }
        } catch (IOException e) {
            log.warn("Không thể tự động mở trình duyệt: {}", e.getMessage());
        }
    }
}
