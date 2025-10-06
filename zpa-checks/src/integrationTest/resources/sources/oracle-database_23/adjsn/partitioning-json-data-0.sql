-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/partitioning-json-data.html
CREATE JSON COLLECTION TABLE orders
  (po_num_vc NUMBER GENERATED ALWAYS AS
    (json_value (DATA, '$.PONumber.number()'
     ERROR ON ERROR))
    MATERIALIZED)
  PARTITION BY RANGE (po_num_vc)
   (PARTITION p1 VALUES LESS THAN (1000),
    PARTITION p2 VALUES LESS THAN (2000));