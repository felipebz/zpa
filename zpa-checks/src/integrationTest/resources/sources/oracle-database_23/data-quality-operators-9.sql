-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/data-quality-operators.html
SQL> select phonic_encode(DOUBLE_METAPHONE,     'phone') c1,
  2         phonic_encode(DOUBLE_METAPHONE_ALT, 'phone') c2 from dual;