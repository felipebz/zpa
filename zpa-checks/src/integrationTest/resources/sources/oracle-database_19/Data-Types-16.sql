-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html
SELECT o.customer_ref.cust_email
  FROM oc_orders o 
  WHERE o.customer_ref IS NOT DANGLING;