-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/INSERT.html
INSERT INTO lob_tab 
   SELECT pic_id, TO_LOB(long_pics) FROM long_tab;