-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE departments ADD
    PARTITION p_3 TABLESPACE tbs3,
    PARTITION p_4 TABLESPACE tbs4;