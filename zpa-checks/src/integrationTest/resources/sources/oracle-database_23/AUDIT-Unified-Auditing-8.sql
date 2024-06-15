-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/AUDIT-Unified-Auditing.html
AUDIT CONTEXT NAMESPACE userenv
  ATTRIBUTES current_user, db_name
  BY hr;