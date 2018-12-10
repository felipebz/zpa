FUNCTION fnc_get_file_separator RETURN VARCHAR2 IS
BEGIN
  IF :PARAMETER.P_WIN_UNIX='WINDOWS' THEN
     RETURN('\');
  ELSE
     RETURN('/');
  END IF;
END fnc_get_file_separator;