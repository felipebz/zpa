-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-AUDIT-POLICY-Unified-Auditing.html
CREATE AUDIT POLICY local_table_pol
       PRIVILEGES CREATE ANY TABLE, DROP ANY TABLE
       CONTAINER = CURRENT;