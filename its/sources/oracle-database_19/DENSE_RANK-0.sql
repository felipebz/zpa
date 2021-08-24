SELECT DENSE_RANK(15500, .05) WITHIN GROUP 
  (ORDER BY salary DESC, commission_pct) "Dense Rank" 
  FROM employees;