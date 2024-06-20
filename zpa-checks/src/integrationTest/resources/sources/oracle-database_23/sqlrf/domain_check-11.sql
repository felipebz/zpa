-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check.html
SELECT DOMAIN_CHECK(not_a_domain, 'raises an error');