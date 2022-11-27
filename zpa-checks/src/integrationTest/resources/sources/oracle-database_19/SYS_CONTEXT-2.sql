-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SYS_CONTEXT.html
SELECT SYS_CONTEXT ('hr_apps', 'group_no') "User Group" 
   FROM DUAL;