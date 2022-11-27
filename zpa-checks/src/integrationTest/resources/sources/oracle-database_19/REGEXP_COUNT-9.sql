-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/REGEXP_COUNT.html
select regexp_count('ABC123A5', '^[A-Z][0-9]') Char_num_like_A1_at_start, 
regexp_count('A1B2C3', '^[A-Z][0-9]') Char_num_like_A1_at_start from dual;