-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/XMLSEQUENCE.html
SELECT EXTRACT(warehouse_spec, '/Warehouse') as "Warehouse"
   FROM warehouses WHERE warehouse_name = 'San Francisco';