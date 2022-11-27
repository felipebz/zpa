-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-PROFILE.html
ALTER PROFILE new_profile 
   LIMIT PASSWORD_REUSE_TIME 90 
   PASSWORD_REUSE_MAX UNLIMITED;