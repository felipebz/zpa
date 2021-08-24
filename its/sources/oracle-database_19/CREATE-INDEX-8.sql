CREATE INDEX area_index ON xwarehouses e 
   (EXTRACTVALUE(VALUE(e),'/Warehouse/Area'));