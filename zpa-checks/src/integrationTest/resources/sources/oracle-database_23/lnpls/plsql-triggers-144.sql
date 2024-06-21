-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-triggers.html
DECLARE
  privilege_list ora_name_list_t;
  number_of_privileges PLS_INTEGER;
BEGIN
  IF (ora_sysevent = 'GRANT' OR
      ora_sysevent = 'REVOKE') THEN
    number_of_privileges :=
      ora_privilege_list(privilege_list);
  END IF;
END;