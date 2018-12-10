FUNCTION Is_DBA
  RETURN boolean
IS
  v_Anzahl number(1);
BEGIN
  select count(*)
    into v_Anzahl
    from User_Role_Privs
   where Granted_Role = 'DBA'
  ;
  if v_Anzahl = 0 then
    return FALSE;
  else
    return TRUE;
  end if;
exception
  when OTHERS then 
    ErrorMessage('Fehler bei "select count(*) from User_Role_Privs".');
    return FALSE;
END;