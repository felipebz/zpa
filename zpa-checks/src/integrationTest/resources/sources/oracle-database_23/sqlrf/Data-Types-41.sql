-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Data-Types.html
CREATE TYPE SDO_TOPO_GEOMETRY AS OBJECT
  (tg_type        NUMBER, 
   tg_id          NUMBER,
   tg_layer_id    NUMBER,
   topology_id    NUMBER);
/