-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-triggers.html
DECLARE
  v_addr VARCHAR2(11);
BEGIN
  IF (ora_sysevent = 'LOGON') THEN
    v_addr := ora_client_ip_address;
  END IF;
END;
/