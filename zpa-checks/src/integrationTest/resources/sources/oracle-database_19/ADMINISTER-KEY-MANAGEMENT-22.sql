-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ADMINISTER-KEY-MANAGEMENT.html
ADMINISTER KEY MANAGEMENT
  EXPORT KEYS WITH SECRET "my_secret"
  TO '/etc/TDE/export.exp'
  IDENTIFIED BY password;