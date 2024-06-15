-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XMLDATA-Pseudocolumn.html
ALTER TABLE xwarehouses
  ADD (UNIQUE(XMLDATA."WarehouseId"));