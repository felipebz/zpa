-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-LOCKDOWN-PROFILE.html
ALTER LOCKDOWN PROFILE hr_prof
  DISABLE FEATURE = ('LOB_FILE_ACCESS', 'TRACE_VIEW_ACCESS');