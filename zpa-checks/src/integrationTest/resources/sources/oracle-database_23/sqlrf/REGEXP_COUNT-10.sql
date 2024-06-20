-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_COUNT.html
select regexp_count('ABC123', '[A-Z][0-9]{2}') Char_num_like_A12_anywhere, 
regexp_count('A1B2C34', '[A-Z][0-9]{2}') Char_num_like_A12_anywhere from dual;