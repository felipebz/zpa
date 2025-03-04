-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/views-information-triggers.html
SELECT Trigger_body
FROM USER_TRIGGERS
WHERE Trigger_name = 'EMP_COUNT';