-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/EXISTSNODE.html
SELECT warehouse_id, warehouse_name
  FROM warehouses
  WHERE EXISTSNODE(warehouse_spec, '/Warehouse/Docks') = 1
  ORDER BY warehouse_id;