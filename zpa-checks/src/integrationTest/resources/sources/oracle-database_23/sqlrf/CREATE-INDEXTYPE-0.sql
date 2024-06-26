-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-INDEXTYPE.html
CREATE INDEXTYPE position_indextype
   FOR position_between(NUMBER, NUMBER, NUMBER)
   USING position_im;