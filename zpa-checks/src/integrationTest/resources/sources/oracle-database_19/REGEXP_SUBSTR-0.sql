-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/REGEXP_SUBSTR.html
SELECT
  REGEXP_SUBSTR('500 Oracle Parkway, Redwood Shores, CA',
                ',[^,]+,') "REGEXPR_SUBSTR"
  FROM DUAL;