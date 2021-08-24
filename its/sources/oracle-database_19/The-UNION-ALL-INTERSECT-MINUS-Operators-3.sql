SELECT location_id, department_name "Department", 
   TO_CHAR(NULL) "Warehouse"  FROM departments
   UNION
   SELECT location_id, TO_CHAR(NULL) "Department", warehouse_name 
   FROM warehouses;