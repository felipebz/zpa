-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-PROFILE.html
ALTER PROFILE app_user LIMIT
   FAILED_LOGIN_ATTEMPTS 5
   PASSWORD_LOCK_TIME 1;