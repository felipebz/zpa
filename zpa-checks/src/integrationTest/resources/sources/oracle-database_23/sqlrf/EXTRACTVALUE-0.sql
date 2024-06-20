-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/EXTRACTVALUE.html
SELECT warehouse_name, EXTRACTVALUE(e.warehouse_spec, '/Warehouse/Docks') "Docks"
  FROM warehouses e 
  WHERE warehouse_spec IS NOT NULL
  ORDER BY warehouse_name;