-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-LOCKDOWN-PROFILE.html
ALTER LOCKDOWN PROFILE hr_prof
  ENABLE STATEMENT ALL EXCEPT = ('ALTER DATABASE');