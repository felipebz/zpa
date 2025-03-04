-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/triggers-publishing-events.html
DECLARE
  user_list ora_name_list_t;
  number_of_grantees PLS_INTEGER;
BEGIN
  IF (ora_sysevent = 'GRANT') THEN
    number_of_grantees := 
     ora_grantee(user_list);
  END IF;
END;