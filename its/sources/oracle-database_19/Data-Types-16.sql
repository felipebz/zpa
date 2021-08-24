SELECT o.customer_ref.cust_email
  FROM oc_orders o 
  WHERE o.customer_ref IS NOT DANGLING;