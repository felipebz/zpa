-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE departments
  SPLIT PARTITION p_1 INTO
   (PARTITION p_1 TABLESPACE tbs1,
    PARTITION p_3 TABLESPACE tbs3)
    UPDATE INDEXES;