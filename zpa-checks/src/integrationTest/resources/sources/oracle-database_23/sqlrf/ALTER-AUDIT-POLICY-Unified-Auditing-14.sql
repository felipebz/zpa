-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-AUDIT-POLICY-Unified-Auditing.html
ALTER AUDIT POLICY employee_audit_policy ACTIONS ADD INSERT(dname) on scott.dept;