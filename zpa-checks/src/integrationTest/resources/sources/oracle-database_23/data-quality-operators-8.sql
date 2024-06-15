-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/data-quality-operators.html
SQL> select phonic_encode(DOUBLE_METAPHONE,     'Schmidt') c1,
  2         phonic_encode(DOUBLE_METAPHONE_ALT, 'Schmidt') c2 from dual;