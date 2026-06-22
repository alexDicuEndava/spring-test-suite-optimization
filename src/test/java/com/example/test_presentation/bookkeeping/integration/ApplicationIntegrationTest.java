package com.example.test_presentation.bookkeeping.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {
		"bookkeeping.demo.startup-delay-enabled=true",
		"bookkeeping.demo.startup-delay=2s"
})
@AutoConfigureMockMvc
@Import(BookkeepingTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
abstract class ApplicationIntegrationTest {
}
