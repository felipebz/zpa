-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/bulk-sql-and-bulk-binding.html
SELECT cust_name "Customer", amount "Valid order amount"
FROM valid_orders
ORDER BY cust_name;