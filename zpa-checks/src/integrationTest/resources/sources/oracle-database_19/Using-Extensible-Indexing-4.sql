-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Using-Extensible-Indexing.html
CREATE INDEXTYPE position_indextype
   FOR position_between(NUMBER, NUMBER, NUMBER)
   USING position_im;