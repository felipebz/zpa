-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check.html
SELECT DOMAIN_CHECK (dgreater, 1, 2) first_lower, 
       DOMAIN_CHECK (dgreater, 2, 1) first_higher,
       DOMAIN_CHECK (dgreater, 'b', 'a') letters;