-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
SELECT cust_name "Customer", amount "Big order amount"
FROM big_orders
ORDER BY cust_name;