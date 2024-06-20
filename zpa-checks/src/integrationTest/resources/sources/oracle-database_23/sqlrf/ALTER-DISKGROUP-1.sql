-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-DISKGROUP.html
ALTER DISKGROUP hmdg ADD FILEGROUP fgtem TEMPLATE SET 'datafile.redundancy'='unprotected'
    ALTER DISKGROUP hmdg ADD FILEGROUP fgdb DATABASE NONE FROM TEMPLATE fgtem