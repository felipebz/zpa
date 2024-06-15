-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_NCHAR-character.html
SELECT TO_NCHAR(cust_last_name) FROM customers
   WHERE customer_id=103;