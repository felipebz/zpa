-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-triggers.html
DECLARE
  user_list ora_name_list_t;
  number_of_users PLS_INTEGER;
BEGIN
  IF (ora_sysevent = 'REVOKE') THEN
    number_of_users := ora_revokee(user_list);
  END IF;
END;