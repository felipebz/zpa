-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check_type.html
SELECT DOMAIN_CHECK_TYPE (dgreater, 1, 2) first_lower, 
       DOMAIN_CHECK_TYPE (dgreater, 2, 1) first_higher,
       DOMAIN_CHECK_TYPE (dgreater, 'b', 'a') letters;