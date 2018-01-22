package org.springframework.boot.configurationalayzer;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link LegacyPropertiesAnalyzerListener}.
 * 
 * @author Stephane Nicoll
 */
public class LegacyPropertiesAnalyzerListenerTests {

	@Rule
	public final OutputCapture output = new OutputCapture();

	private ConfigurableApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void sampleReport() {
		this.context = createSampleApplication()
				.run("--banner.charset=UTF8");
		assertThat(this.output.toString()).contains("commandLineArgs")
				.contains("spring.banner.charset")
				.contains("Each configuration key has been temporarily mapped")
				.doesNotContain("Please refer to the migration guide");
	}

	private SpringApplication createSampleApplication() {
		return new SpringApplication(TestApplication.class);
	}


	@Configuration
	public static class TestApplication {

	}

}
