-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CALL.html
CREATE OR REPLACE FUNCTION ret_warehouse_typ(x warehouse_typ) 
  RETURN warehouse_typ
  IS
    BEGIN
      RETURN x;
    END;
/
CALL ret_warehouse_typ(warehouse_typ(234, 'Warehouse 234',
   2235)).ret_name()
   INTO :x;
PRINT x;