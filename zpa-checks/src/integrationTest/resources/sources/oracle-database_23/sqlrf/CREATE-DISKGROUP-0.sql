-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-DISKGROUP.html
CREATE DISKGROUP dgroup_01
  EXTERNAL REDUNDANCY
  DISK '/devices/disks/c*';