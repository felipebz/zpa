-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CON_UID_TO_ID.html
SELECT CON_ID, CON_UID
  FROM V$CONTAINERS;