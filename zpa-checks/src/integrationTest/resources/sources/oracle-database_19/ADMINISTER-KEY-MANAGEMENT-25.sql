-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ADMINISTER-KEY-MANAGEMENT.html
ADMINISTER KEY MANAGEMENT
  SET ENCRYPTION KEY IDENTIFIED BY "user_id:password"
  MIGRATE USING software_keystore_password
  WITH BACKUP;