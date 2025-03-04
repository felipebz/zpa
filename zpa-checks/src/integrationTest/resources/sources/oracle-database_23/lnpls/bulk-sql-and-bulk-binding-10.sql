-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/bulk-sql-and-bulk-binding.html
SELECT cust_name "Customer", amount "Big order amount"
FROM big_orders
ORDER BY cust_name;