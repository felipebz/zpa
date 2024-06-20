-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-LOCKDOWN-PROFILE.html
ALTER LOCKDOWN PROFILE hr_prof
  ENABLE STATEMENT = ('ALTER SESSION')
         CLAUSE = ('SET')
         OPTION = ('COMMIT_WAIT', 'CURSOR_SHARING');