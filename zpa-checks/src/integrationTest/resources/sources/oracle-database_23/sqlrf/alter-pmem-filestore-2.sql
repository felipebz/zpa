-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/alter-pmem-filestore.html
ALTER  PMEM FILESTORE cloud_db_1 MOUNT  MOUNTPOINT ‘/corp/db/cloud_db_1’
    BACKINGFILE ‘/var/pmem/foo_1’