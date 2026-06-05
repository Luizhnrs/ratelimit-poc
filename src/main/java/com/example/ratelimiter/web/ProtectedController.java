package com.example.ratelimiter.web;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProtectedController {

    private final String instanceName;

    public ProtectedController() {
        this.instanceName = resolveInstanceName();
    }

    @GetMapping("/api/protected")
    Map<String, String> protectedEndpoint() {
        return Map.of(
                "message", "Request accepted",
                "instance", instanceName
        );
    }

    private static String resolveInstanceName() {
        var configured = System.getenv("HOSTNAME");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }
}
