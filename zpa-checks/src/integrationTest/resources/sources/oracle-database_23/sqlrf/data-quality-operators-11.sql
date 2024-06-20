-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/data-quality-operators.html
SQL> -- PNNT / PKNNT
SQL> select phonic_encode(DOUBLE_METAPHONE,     'poignant') c1,
  2         phonic_encode(DOUBLE_METAPHONE_ALT, 'poignant') c2,
  3         phonic_encode(DOUBLE_METAPHONE_ALT, 'poignant', 10) c3 from dual;