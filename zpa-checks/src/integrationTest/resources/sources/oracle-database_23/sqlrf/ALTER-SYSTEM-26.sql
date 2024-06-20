-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-SYSTEM.html
SELECT sid, serial#, username
   FROM V$SESSION;