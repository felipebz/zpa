-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_SUBSTR.html
SELECT
  REGEXP_SUBSTR('http://www.example.com/products',
                'http://([[:alnum:]]+\.?){3,4}/?') "REGEXP_SUBSTR"
  FROM DUAL;