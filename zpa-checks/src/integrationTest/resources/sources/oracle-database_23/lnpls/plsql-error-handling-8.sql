-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-error-handling.html
CALL DBMS_WARNING.set_warning_setting_string ('ENABLE:ALL', 'SESSION');