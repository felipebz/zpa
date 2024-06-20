-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/EXTRACT-XML.html
SELECT warehouse_name,
       EXTRACT(warehouse_spec, '/Warehouse/Docks') "Number of Docks"
  FROM warehouses
  WHERE warehouse_spec IS NOT NULL
  ORDER BY warehouse_name;