package com.tradeguard.testsuite.bdd;

import com.tradeguard.sutapi.TradeGuardApiApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the real application (embedded in-memory DB, "test" profile) on a random
 * port so step definitions drive it over HTTP exactly like a live client would.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = TradeGuardApiApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}
