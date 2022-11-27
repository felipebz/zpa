-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ADMINISTER-KEY-MANAGEMENT.html
SELECT KEY_ID FROM V$ENCRYPTION_KEYS WHERE TAG LIKE 'mytag%'