-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
INSERT INTO orders VALUES
  (1, 'Costco', order_status.open ),
  (2, 'BMW', order_status.closed ),
  (3, 'Nestle', order_status.shipped );