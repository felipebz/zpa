-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/data-quality-operators.html
SQL> select fuzzy_match(LEVENSHTEIN, 'Mohamed Tarik', 'Mo Tariq', unscaled) from dual;