package com.coursepulse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class CoursepulseApplicationTests {

	@Test
	void contextLoads() {
		// Verifies that the application context starts successfully.
	}

}
