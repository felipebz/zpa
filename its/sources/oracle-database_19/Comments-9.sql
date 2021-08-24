SELECT /*+ DYNAMIC_SAMPLING(e 1) */ count(*)
  FROM employees e;