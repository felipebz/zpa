-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-AUDIT-POLICY-Unified-Auditing.html
CREATE AUDIT POLICY employee_audit_policy ACTIONS SELECT(sal) on scott.emp;