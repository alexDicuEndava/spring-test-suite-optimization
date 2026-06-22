INSERT INTO customers (id, name, email, status) VALUES (300, 'Acme Books', 'reports-acme@example.test', 'ACTIVE');
INSERT INTO customers (id, name, email, status) VALUES (301, 'Beacon Retail', 'reports-beacon@example.test', 'ACTIVE');
INSERT INTO customers (id, name, email, status) VALUES (302, 'Cobalt Services', 'reports-cobalt@example.test', 'INACTIVE');
INSERT INTO customers (id, name, email, status) VALUES (303, 'Delta Archive', 'reports-delta@example.test', 'ACTIVE');

INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (300, 'INV-AGE-001', 300, 'SENT', DATE '2026-02-01', DATE '2026-06-22', 120.00);
INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (301, 'INV-AGE-002', 300, 'DRAFT', DATE '2026-02-02', DATE '2026-06-01', 80.00);
INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (302, 'INV-AGE-003', 300, 'SENT', DATE '2026-02-03', DATE '2026-05-01', 60.00);
INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (303, 'INV-AGE-004', 300, 'DRAFT', DATE '2026-02-04', DATE '2026-03-01', 40.00);
INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (304, 'INV-AGE-005', 300, 'PAID', DATE '2026-02-05', DATE '2026-02-01', 999.00);
INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (305, 'INV-AGE-006', 300, 'VOID', DATE '2026-02-06', DATE '2026-01-01', 888.00);

INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (306, 'INV-AGE-007', 301, 'SENT', DATE '2026-03-01', DATE '2026-06-23', 200.00);
INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (307, 'INV-AGE-008', 301, 'SENT', DATE '2026-03-02', DATE '2026-06-21', 30.00);
INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (308, 'INV-AGE-009', 301, 'DRAFT', DATE '2026-03-03', DATE '2026-04-23', 70.00);

INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (309, 'INV-AGE-010', 302, 'SENT', DATE '2026-04-01', DATE '2026-06-15', 11.00);
INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (310, 'INV-AGE-011', 302, 'DRAFT', DATE '2026-04-02', DATE '2026-05-22', 22.00);
INSERT INTO invoices (id, invoice_number, customer_id, status, issue_date, due_date, total)
VALUES (311, 'INV-AGE-012', 302, 'SENT', DATE '2026-04-03', DATE '2026-04-21', 33.00);
