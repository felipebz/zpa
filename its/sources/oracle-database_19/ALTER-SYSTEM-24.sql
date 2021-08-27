-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-SYSTEM.html
SELECT sid, serial#, username
   FROM V$SESSION; 