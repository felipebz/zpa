-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/data-quality-operators.html
SQL> select phonic_encode(DOUBLE_METAPHONE,     'George') c1,
  2         phonic_encode(DOUBLE_METAPHONE_ALT, 'George') c2 from dual;