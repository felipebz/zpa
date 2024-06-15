-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/The-UNION-ALL-INTERSECT-MINUS-Operators.html
SELECT location_id, department_name "Department", 
   TO_CHAR(NULL) "Warehouse"  FROM departments
   UNION
   SELECT location_id, TO_CHAR(NULL) "Department", warehouse_name 
   FROM warehouses;