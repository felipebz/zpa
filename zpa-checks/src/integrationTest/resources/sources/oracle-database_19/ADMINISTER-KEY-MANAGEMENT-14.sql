-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ADMINISTER-KEY-MANAGEMENT.html
ADMINISTER KEY MANAGEMENT
  MERGE KEYSTORE '/etc/ORACLE/KEYSTORE/DB1'
  INTO EXISTING KEYSTORE '/etc/ORACLE/KEYSTORE/DB2'
    IDENTIFIED BY existing_keystore_password
  WITH BACKUP;