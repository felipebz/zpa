-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_COUNT.html
select regexp_count('ABC123', '[A-Z][0-9]'), regexp_count('A1B2C3', '[A-Z][0-9]') from dual;