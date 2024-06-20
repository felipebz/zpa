-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-INDEX.html
CREATE INDEX area_index ON xwarehouses e 
   (EXTRACTVALUE(VALUE(e),'/Warehouse/Area'));