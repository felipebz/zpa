-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE TABLE part_staging_table (col1 number, col2 varchar2(100))
PARTITION BY RANGE (col1) (PARTITION p1 VALUES LESS THAN (100), PARTITION pmax VALUES LESS THAN (MAXVALUE)) 
FOR STAGING;