package com.tradeguard.testsuite.bdd;

import com.tradeguard.sutapi.TradeGuardApiApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the real Spring context (embedded H2) on a random port so that
 * step definitions can drive the API exactly like a live client would,
 * using REST Assured over HTTP.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = TradeGuardApiApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    public int getPort() {
        return port;
    }
}
