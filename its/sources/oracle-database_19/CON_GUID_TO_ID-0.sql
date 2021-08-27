-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CON_GUID_TO_ID.html
SELECT CON_ID, GUID
  FROM V$CONTAINERS;