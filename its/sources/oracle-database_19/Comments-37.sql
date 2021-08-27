-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comments.html
SELECT /*+NO_XMLINDEX_REWRITE*/ count(*) 
  FROM warehouses
  WHERE existsNode(warehouse_spec, '/Warehouse/Building') = 1;