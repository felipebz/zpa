-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-DISKGROUP.html
ALTER DISKGROUP hmdg ADD FILEGROUP fgtem2 TEMPLATE 
    CREATE TABLESPACE tbs1 datafile '+hmdg(fg$fgtem2)/dbs/tbs1.f' size 1M