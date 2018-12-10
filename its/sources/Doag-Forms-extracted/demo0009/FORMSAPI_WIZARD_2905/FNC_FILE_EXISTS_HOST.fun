FUNCTION fnc_File_Exists_HOST(p_fname VARCHAR2) RETURN BOOLEAN IS
  l_file Text_IO.File_Type;
  
BEGIN
  
  l_file  := Text_IO.Fopen(p_fname, 'r');
  Text_IO.Fclose(l_file);
  
  RETURN(TRUE);
  
EXCEPTION WHEN OTHERS THEN
  RETURN(FALSE);
END fnc_File_Exists_HOST;