-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-AUDIT-POLICY-Unified-Auditing.html
CREATE AUDIT POLICY mypolicy ACTIONS COMPONENT = PROTOCOL FTP
    AUDIT POLICY mypolicy WHENEVER NOT SUCCESSFUL