-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-USER.html
ALTER USER app_user1 
   GRANT CONNECT THROUGH sh
   WITH ROLE warehouse_user;