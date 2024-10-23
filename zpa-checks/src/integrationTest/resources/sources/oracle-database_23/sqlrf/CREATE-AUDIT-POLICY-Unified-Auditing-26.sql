-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-AUDIT-POLICY-Unified-Auditing.html
CREATE AUDIT POLICY emp_updates_pol
        ACTIONS DELETE on hr.employees,
          INSERT on hr.employees,
          UPDATE on hr.employees
        WHEN 'UID NOT IN (100, 105, 107)'
        EVALUATE PER STATEMENT;