-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/VAR_POP.html
SELECT t.calendar_month_desc,
   VAR_POP(SUM(s.amount_sold)) 
      OVER (ORDER BY t.calendar_month_desc) "Var_Pop",
   VAR_SAMP(SUM(s.amount_sold)) 
      OVER (ORDER BY t.calendar_month_desc) "Var_Samp" 
  FROM sales s, times t
  WHERE s.time_id = t.time_id AND t.calendar_year = 1998
  GROUP BY t.calendar_month_desc
  ORDER BY t.calendar_month_desc, "Var_Pop", "Var_Samp";