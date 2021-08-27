-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CORR_A.html
SELECT CORR_K(salary, commission_pct, 'COEFFICIENT') coefficient,
       CORR_K(salary, commission_pct, 'TWO_SIDED_SIG') two_sided_p_value
  FROM employees;