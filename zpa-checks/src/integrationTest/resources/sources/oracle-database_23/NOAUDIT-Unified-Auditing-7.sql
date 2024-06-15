-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NOAUDIT-Unified-Auditing.html
NOAUDIT CONTEXT NAMESPACE userenv
  ATTRIBUTES current_user, db_name
  BY hr;