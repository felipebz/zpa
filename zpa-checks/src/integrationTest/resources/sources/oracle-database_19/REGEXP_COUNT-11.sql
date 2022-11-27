-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/REGEXP_COUNT.html
select regexp_count('ABC12D3', '([A-Z][0-9]){2}') Char_num_within_2_places, 
regexp_count('A1B2C3', '([A-Z][0-9]){2}') Char_num_within_2_places from dual;