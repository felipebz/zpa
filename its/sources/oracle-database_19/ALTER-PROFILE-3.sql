-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-PROFILE.html
ALTER PROFILE app_user2 LIMIT
   PASSWORD_LIFE_TIME 90
   PASSWORD_GRACE_TIME 5;