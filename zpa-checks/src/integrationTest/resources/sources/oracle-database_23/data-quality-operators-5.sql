-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/data-quality-operators.html
SQL> select fuzzy_match(WHOLE_WORD_MATCH,  'Mohamed Tarik', 'Mo Tariq') from dual;