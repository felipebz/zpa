-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-PLUGGABLE-DATABASE.html
CREATE PLUGGABLE DATABASE cdb1_pdb3
    ADMIN USER IDENTIFIED BY manager
    FILE_NAME_CONVERT=('cdb1_pdb0, cdb1_pdb3')
    CONTAINER_MAP UPDATE (ADD PARTITION cdb1_pdb3 VALUES LESS THAN (100));
    ALTER PLUGGABLE DATABASE cdb1_pdb3 OPEN