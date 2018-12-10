PACKAGE pkg_FAM_run_Script IS
  
  
  -- F.Matz: 15.01.2017 , 21.02.2017
  
  PROCEDURE prc_run_batch_version_cmd (p_srcfile VARCHAR2);
  
  PROCEDURE prc_run_batch_comp_forms_cmd (p_srcfile VARCHAR2, p_compfile VARCHAR2, p_type VARCHAR2 DEFAULT 'FORM');
 
  PROCEDURE prc_run_batch_comp_reports_cmd (p_srcfile VARCHAR2, p_compfile VARCHAR2, p_type VARCHAR2 DEFAULT 'REPORTS');
 
  PROCEDURE prc_run_batch_compile_cmd (p_srcfile VARCHAR2);
  
  PROCEDURE prc_run_FAM_script_mig(p_script_name VARCHAR2);
  
  PROCEDURE prc_run_FAM_script_win2unix(p_script_name VARCHAR2);
  
  PROCEDURE prc_run_FAM_check_script(p_srcfile VARCHAR2);
  
  PROCEDURE prc_wrt_CMD(p_cmd_name VARCHAR2, p_script VARCHAR2);
  
  
  
END pkg_FAM_run_Script;