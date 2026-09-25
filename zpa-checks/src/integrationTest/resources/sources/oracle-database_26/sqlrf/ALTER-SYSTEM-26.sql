-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SYSTEM.html
SELECT sid, serial#, username
   FROM V$SESSION;