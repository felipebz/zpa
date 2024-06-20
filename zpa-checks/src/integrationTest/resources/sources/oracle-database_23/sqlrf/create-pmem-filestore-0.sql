-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-pmem-filestore.html
CREATE PMEM FILESTORE cloud_db_1 MOUNTPOINT ‘/corp/db/cloud_db_1’ 
    BACKINGFILE ‘/var/pmem/foo_1.’ SIZE 2T BLOCKSIZE 8K 
    AUTOEXTEND ON NEXT 10G MAXSIZE 3T