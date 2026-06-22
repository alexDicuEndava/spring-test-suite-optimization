package com.example.test_presentation.bookkeeping.integration;

import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {
		"/sql/bookkeeping-cleanup.sql",
		"/sql/customers-create.sql",
		"/sql/invoices-create.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
abstract class ReadOnlyBookkeepingApiTestSupport extends BookkeepingApiTestSupport {
}
