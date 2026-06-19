package com.example.test_presentation.bookkeeping;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
		"bookkeeping.demo.startup-delay-enabled=true",
		"bookkeeping.demo.startup-delay=500ms"
})
@AutoConfigureMockMvc
@Import(StableBookkeepingTestConfiguration.class)
abstract class OptimizedIntegrationTest {
}
