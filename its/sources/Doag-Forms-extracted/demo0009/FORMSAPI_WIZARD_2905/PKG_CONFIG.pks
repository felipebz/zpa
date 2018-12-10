PACKAGE pkg_CONFIG IS
  
  -- F.Matz: 15.01.2017, 26.04.2017 --
  
  PROCEDURE prc_create_DIRS;
  
  PROCEDURE prc_create_DIRS_HOST;

  PROCEDURE prc_set_Config_INI;
  
  PROCEDURE prc_get_config(p_fname VARCHAR2);
  
  PROCEDURE prc_set_Config_INI_Source(p_srcfile VARCHAR2);
  
  PROCEDURE prc_Copy_Dir(p_srcdir VARCHAr2, p_destdir VARCHAR2);
  
  PROCEDURE prc_Copy_Dir_HOST(p_srcdir VARCHAR2, p_destdir VARCHAR2);
  
  PROCEDURE prc_Copy_Dir_RDF(p_srcdir VARCHAR2, p_destdir VARCHAR2);
  
  PROCEDURE prc_Copy_Dir_RDF_HOST(p_srcdir VARCHAR2, p_destdir VARCHAR2);
  
END pkg_CONFIG;