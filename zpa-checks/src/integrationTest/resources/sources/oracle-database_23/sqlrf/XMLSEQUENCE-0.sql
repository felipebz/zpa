-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XMLSEQUENCE.html
SELECT EXTRACT(warehouse_spec, '/Warehouse') as "Warehouse"
   FROM warehouses WHERE warehouse_name = 'San Francisco';
SELECT VALUE(p)
   FROM warehouses w, 
   TABLE(XMLSEQUENCE(EXTRACT(warehouse_spec, '/Warehouse/*'))) p
   WHERE w.warehouse_name = 'San Francisco';