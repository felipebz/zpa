-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/constraint.html
ALTER TABLE warehouses
   ADD CONSTRAINT wh_unq UNIQUE (warehouse_id, warehouse_name)
   USING INDEX PCTFREE 5
   EXCEPTIONS INTO wrong_id;