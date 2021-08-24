SELECT COUNT(*) 
  FROM employees 
  WHERE TO_BINARY_FLOAT(commission_pct)
     != BINARY_FLOAT_NAN;