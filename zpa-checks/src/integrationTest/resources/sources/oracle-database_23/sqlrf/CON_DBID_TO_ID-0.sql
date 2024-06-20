-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CON_DBID_TO_ID.html
SELECT CON_ID, DBID
  FROM V$CONTAINERS;