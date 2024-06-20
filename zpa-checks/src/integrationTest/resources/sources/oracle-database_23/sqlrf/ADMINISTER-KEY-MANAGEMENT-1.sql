-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ADMINISTER-KEY-MANAGEMENT.html
ADMINISTER KEY MANAGEMENT
  USE ENCRYPTION KEY '0673C1262AA1D04F14BF26D720480C55B2'
  IDENTIFIED BY "external_keystore_password"
  MIGRATE USING software_keystore_password;