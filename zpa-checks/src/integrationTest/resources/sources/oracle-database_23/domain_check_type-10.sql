-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check_type.html
SELECT DOMAIN_CHECK_TYPE(not_a_domain, 'raises an error');
ORA-11504: The domain specified does not exist or the user does not have privileges on the domain for the operation.