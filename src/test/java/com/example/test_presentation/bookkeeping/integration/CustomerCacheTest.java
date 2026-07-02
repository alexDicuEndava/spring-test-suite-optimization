package com.example.test_presentation.bookkeeping.integration;

import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.repository.CustomerRepository;
import com.example.test_presentation.bookkeeping.service.CustomerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "bookkeeping.demo.startup-delay-enabled=true",
        "bookkeeping.demo.startup-delay=3s"
})
@AutoConfigureMockMvc
@Import(BookkeepingTestConfiguration.class)
@DirtiesContext
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/customers-create.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CustomerCacheTest {

    @Autowired
    CustomerService customerService;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    CacheManager cacheManager;

    @AfterEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @ParameterizedTest(name = "clears customer cache {0}")
    @CsvSource({"Acme Books Updated Directly", "Acme Books Updated Again"})
    void clearsCacheExplicitlyInsteadOfDirtyingContext(String updatedName) {
        assertThat(customerService.find(100L).name()).isEqualTo("Acme Books");

        Customer customer = customerRepository.findById(100L).orElseThrow();
        customer.setName(updatedName);
        customerRepository.saveAndFlush(customer);

        assertThat(customerService.find(100L).name()).isEqualTo("Acme Books");
        clearCaches();
        assertThat(customerService.find(100L).name()).isEqualTo(updatedName);
    }
}
