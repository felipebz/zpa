SELECT /*+NO_XMLINDEX_REWRITE*/ count(*) 
  FROM warehouses
  WHERE existsNode(warehouse_spec, '/Warehouse/Building') = 1;