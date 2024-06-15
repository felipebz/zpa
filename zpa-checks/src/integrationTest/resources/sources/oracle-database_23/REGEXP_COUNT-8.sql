-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_COUNT.html
select regexp_count('ABC123', '[A-Z][0-9]') Match_string_C1_count, 
regexp_count('A1B2C3', '[A-Z][0-9]')  Match_strings_A1_B2_C3_count from dual;