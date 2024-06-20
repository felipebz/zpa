-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SYS_CONTEXT.html
SELECT SYS_CONTEXT ('hr_apps', 'group_no') "User Group" 
   FROM DUAL;