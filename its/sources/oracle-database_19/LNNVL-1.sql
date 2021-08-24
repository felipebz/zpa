SELECT COUNT(*)
  FROM employees
  WHERE LNNVL(commission_pct >= .2);