-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/data-quality-operators.html
SQL> select fuzzy_match(LONGEST_COMMON_SUBSTRING, 'Mohamed Tarik', 'Mo Tariq', unscaled) from dual;