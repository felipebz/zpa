PACKAGE BODY pkg_FAM_run_Script IS
  
  -- F.Matz: 15.01.2017 / 10.02.2017 / FH 4.3.2017 / FM 22.03.2017
  
  -- C_SCRIPT_ONE_UPGR)  := 'CMD /C call ${p_formsapi_home} /SCRIPT /API=11g /RUN="${p_wsp_prj_scripts_upgr}\${p_script_name}" '; --/INFILES="${p_wsp_prj_souces}\${p_srcfile}" /DEBUGLOG="${p_wsp_logs_upgr}\${p_srcfile}.log" ';
  
  C_BATCH_VERSION_def CONSTANT VARCHAR2(2048) := '${p_config_formsapi_execute} /BATCH /MODE=CHECKVERSION /FILES="${p_source_dir}\${p_srcfile}" /TARGETDIR="${p_script_dir}" /OUT="${p_script_dir}\check.txt" ';

  C_FAM_SCRIPT_chkref CONSTANT VARCHAR2(2048):=
  'DECLARE
  frm : number;
  // --- Main
BEGIN
  logadd(\''--- BO Start Check - Subclass References ---\'');
  API_IgnoreMissingReferences(false);
  try
     frm := API_LoadModule(\''${p_source_dir}\${p_srcfile}\'');
  except
     logadd(\''$$$ EXCEPTION: Fehler beim Modul Laden - Subclassing ?  $$$\'');
     RETURN;
  end;
  logadd(\''--- EO Start Check - Subclass References ---\'');
  API_DestroyModule(frm);
END; // --- Main ';

  -- check 'references' script --
  C_CMD_RUN_SCRIPT_chkref CONSTANT VARCHAR2(2048):= 
    'SET FORMS_PATH=${p_source_dir}'||chr(10)||
    'CD ${p_addon_dir}'||chr(10)||
    '"${p_config_formsapi_execute}" /SCRIPT /API=12c /RUN="${p_script_dir}\${p_script_name}" ';
    
  -- migration scripts         --
  C_CMD_RUN_SCRIPT_def_mig CONSTANT VARCHAR2(2048):= 
    '${p_source_network}'||chr(10)||
    'CD ${p_addon_dir}'||chr(10)||
    '"${p_config_formsapi_execute}" /SCRIPT /API=12c /RUN="${p_script_dir}\${p_script_name}" ';
  
    -- migration scripts         --
  C_CMD_RUN_SCRIPT_def_win2unix CONSTANT VARCHAR2(2048):= 
    'SET FORMS_PATH=${p_addon_dir}'||chr(10)||
    '"${p_config_formsapi_execute}" /SCRIPT /API=12c /RUN="${p_script_dir}\${p_script_name}" ';
 
   
  -- Migration
  -- cmd /c call %cddir_installfam% /script /API=11g /HOMES="c:\oracle\weblogic1036\Oracle_FRHome1\BIN" /RUN=%cddir_skripte%\mig_main_one.p2s /LOG=%cddir_skripte%\log_migration_one_fmb.txt
  -- echo Win2Unix Verarbeitung
  -- c:
  -- cd %cddir%
  -- cmd /c call %cddir_installfam% /script /API=11g /HOMES="c:\oracle\weblogic1036\Oracle_FRHome1\BIN" /RUN=%cddir_skripte%\mig_Win2Unix_one.p2s /LOG=%cddir_skripte%\log_win2unix_one.txt
  
  C_BATCH_COMPILE_def CONSTANT VARCHAR2(2048):= 
    '${p_source_network}'||chr(10)||
    'CD ${p_addon_dir}'||chr(10)||
    '${p_config_formsapi_execute} /BATCH /MODE=COMPILE /COMPILEALL=Y /USERID="${p_db_connect}" /FILES="${p_unix_dir}\${p_srcfile}" /TARGETDIR="${p_comp_dir}" /OUT="${p_log_dir}\${p_log_file}" ';

  C_BATCH_FORMS_COMPILE_def CONSTANT VARCHAR2(2048):= 
    '${p_source_network}'||chr(10)||
    'CD ${p_addon_dir}'||chr(10)||
    'frmcmp "${p_unix_dir}\${p_srcfile}" userid=${p_db_connect} module_type=${p_type} batch=yes window_state=minimize output_file="${p_comp_dir}\${p_compfile}" compile_all=yes';

  C_BATCH_REPORTS_COMPILE_def CONSTANT VARCHAR2(2048):= 
    '${p_source_network}'||chr(10)||
    'CD ${p_addon_dir}'||chr(10)||
    'rwconverter userid=${p_db_connect} stype=RDFFILE source="${p_unix_dir}\${p_srcfile}" dest="${p_comp_dir}\${p_compfile}" dtype=REPFILE overwrite=YES compile_all=yes batch=YES';

  m_CMD_RUN_chkref     VARCHAR2(2048);
  m_CMD_RUN_SCRIPT      VARCHAR2(2048); 
  m_FAM_SCRIPT_chkref  VARCHAR2(4096);
  
  m_BATCH_VERSION_run  VARCHAR2(2048);
  m_BATCH_COMPILE_run  VARCHAR2(4096);
  
  m_log       CONSTANT BOOLEAN := FALSE;
  
-------------------------------------------------------------------------------------------------------  
  PROCEDURE prc_iflog (p_log     VARCHAR2,    
                       p_1      VARCHAR2 DEFAULT NULL, 
                        p_2      VARCHAR2 DEFAULT NULL, 
                       p_3      VARCHAR2 DEFAULT NULL, 
                       p_4      VARCHAR2 DEFAULT NULL, 
                        p_5      VARCHAR2 DEFAULT NULL, 
                       p_msg2    VARCHAR2 DEFAULT '.',
                       p_TYPE   VARCHAR2 DEFAULT 'INFO') IS
  BEGIN
     IF m_log THEN
        prc_flog(p_log, p_1, p_2, p_3, p_4, p_5, p_msg2, p_TYPE);
     END IF;
  END prc_iflog;

-------------------------------------------------------------------------------------------------------    
  PROCEDURE prc_wrt_CMD(p_cmd_name VARCHAR2, p_script VARCHAR2) IS
     l_io_file    Text_IO.File_Type;
     l_io_file_C  client_Text_IO.File_Type;
  BEGIN
     g.msg_push('pkg_run_FAM_script.prc_wrt_CMD');
     
     IF :PARAMETER.P_HOST_CLIENT='HOST' THEN 
        SYNCHRONIZE;
        l_io_file := Text_IO.Fopen(:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||p_cmd_name, 'w');
        Text_IO.put_line(l_io_file, p_script);                                                                         
        Text_IO.fclose  (l_io_file);
        SYNCHRONIZE;
     
     ELSIF :PARAMETER.P_HOST_CLIENT='CLIENT' THEN 
        SYNCHRONIZE;
        l_io_file_C := client_Text_IO.Fopen(:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||p_cmd_name, 'w');
        client_Text_IO.put_line(l_io_file_C, p_script);                                                                         
        client_Text_IO.fclose  (l_io_file_C);
        SYNCHRONIZE;
     
     END IF;
     
      g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_wrt_CMD;

-------------------------------------------------------------------------------------------------------    
  PROCEDURE prc_run_batch_version_cmd (p_srcfile VARCHAR2) IS
    -- p_batch : dummy .
  BEGIN
    g.msg_push('pkg_FAM_run_Script.prc_run_batch_version_cmd');  
      
    -- save actual source file --
    :PARAMETER.P_SOURCE_FILE:= p_srcfile;
    
    m_BATCH_VERSION_run:= C_BATCH_VERSION_def;

    m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_source_network}',             substr(:PARAMETER.P_ADDON_DIR,1,2));   
    m_BATCH_VERSION_run:= replace(m_BATCH_VERSION_run, '${p_config_formsapi_execute}',   :PARAMETER.P_CONFIG_FORMSAPI_EXECUTE);
    m_BATCH_VERSION_run:= replace(m_BATCH_VERSION_run, '${p_srcfile}',                   p_srcfile);
    m_BATCH_VERSION_run:= replace(m_BATCH_VERSION_run, '${p_source_dir}',               :PARAMETER.P_SOURCE_DIR);
     m_BATCH_VERSION_run:= replace(m_BATCH_VERSION_run, '${p_script_dir}',                :PARAMETER.P_SCRIPT_DIR);
    
     prc_iflog('--- BO SCRIP-CHECK : '||  m_BATCH_VERSION_run);
     
     -- 22.03.2017 / 26.04.2017 -- 
     IF :PARAMETER.P_HOST_CLIENT='CLIENT' THEN 
       WEBUTIL_HOST.BLOCKING(  m_BATCH_VERSION_run );    
     ELSIF :PARAMETER.P_HOST_CLIENT='HOST' THEN      
       HOST( m_BATCH_VERSION_run, NO_SCREEN );  
     END IF;
     
    g.msg_free;
    
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_run_batch_version_cmd;
  
  ----------------------------------------------------------------------------------------------------------------------------
  --- ACHTUNG: SOURCE-DIR := UNIX-DIR !!! <<<<<<<<<<<<<<<<<<<
  -----------------------------------------------------------
  PROCEDURE prc_run_batch_comp_forms_cmd (p_srcfile VARCHAR2, p_compfile VARCHAR2, p_type VARCHAR2 DEFAULT 'FORM') IS
    -- p_batch : dummy .
  BEGIN
    g.msg_push('pkg_FAM_run_Script.prc_run_batch_compile_forms_cmd');  
      
    -- save actual source file --
    :PARAMETER.P_SOURCE_FILE:= p_srcfile;
    
    ------------------- BO - write BATCH ---------------------
    m_BATCH_COMPILE_run:= C_BATCH_FORMS_COMPILE_def;
      
     -- m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_config_formsapi_execute}',   :PARAMETER.P_CONFIG_FORMSAPI_EXECUTE);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_source_network}',             substr(:PARAMETER.P_ADDON_DIR,1,2));
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_source_dir}',                :PARAMETER.P_UNIX_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_addon_dir}',                :PARAMETER.P_ADDON_DIR);
    m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_srcfile}',                   p_srcfile);
    m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_unix_dir}',                 :PARAMETER.P_UNIX_DIR);
     -- m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_script_dir}',                :PARAMETER.P_SCRIPT_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_db_connect}',                :PARAMETER.P_DB_CONNECT);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_comp_dir}',                  :PARAMETER.P_COMP_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_compfile}',                   p_compfile);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_type}',                       p_type);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_log_dir}',                  :PARAMETER.P_LOG_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_log_file}',                   p_srcfile||'-compile.txt');

     prc_wrt_CMD('do_forms_compile.cmd', m_BATCH_COMPILE_run);
     
     SYNCHRONIZE; -- ??? --
      
     ------------------- EO - write BATCH ---------------------
      
     prc_iflog('--- BO BATCH-COMPILE : '||  m_BATCH_COMPILE_run);
     
    -- 22.03.2017 -- 
    IF :PARAMETER.P_HOST_CLIENT='CLIENT' THEN   
       WEBUTIL_HOST.BLOCKING( 'CMD /C call ' || :PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'do_forms_compile.cmd' ); 
     ELSIF  :PARAMETER.P_HOST_CLIENT='HOST' THEN     
       HOST('CMD /C call ' || :PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'do_forms_compile.cmd' , NO_SCREEN );
     END IF;
     
    g.msg_free;
    
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_run_batch_comp_forms_cmd;

-------------------------------------------------------------------------------------------------------    
  PROCEDURE prc_run_batch_comp_reports_cmd (p_srcfile VARCHAR2, p_compfile VARCHAR2, p_type VARCHAR2 DEFAULT 'REPORTS') IS
    -- p_batch : dummy .
  BEGIN
    g.msg_push('pkg_FAM_run_Script.prc_run_batch_compile_reports_cmd');  
      
    -- save actual source file --
    :PARAMETER.P_SOURCE_FILE:= p_srcfile;
    
    ------------------- BO - write BATCH ---------------------
    m_BATCH_COMPILE_run:= C_BATCH_REPORTS_COMPILE_def;
      
     -- m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_config_formsapi_execute}',   :PARAMETER.P_CONFIG_FORMSAPI_EXECUTE);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_source_network}',             substr(:PARAMETER.P_ADDON_DIR,1,2));
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_source_dir}',                :PARAMETER.P_UNIX_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_addon_dir}',                :PARAMETER.P_ADDON_DIR);
    m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_srcfile}',                   p_srcfile);
    m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_unix_dir}',                 :PARAMETER.P_UNIX_DIR);
     -- m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_script_dir}',                :PARAMETER.P_SCRIPT_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_db_connect}',                :PARAMETER.P_DB_CONNECT);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_comp_dir}',                  :PARAMETER.P_COMP_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_compfile}',                   p_compfile);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_type}',                       p_type);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_log_dir}',                  :PARAMETER.P_LOG_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_log_file}',                   p_srcfile||'-compile.txt');

     prc_wrt_CMD('do_reports_compile.cmd', m_BATCH_COMPILE_run);
     
     SYNCHRONIZE; -- ??? --
      
     ------------------- EO - write BATCH ---------------------
      
     prc_iflog('--- BO BATCH-COMPILE : '||  m_BATCH_COMPILE_run);
     
    -- 22.03.2017 -- 
    IF :PARAMETER.P_HOST_CLIENT='CLIENT' THEN      
        WEBUTIL_HOST.BLOCKING( 'CMD /C call ' || :PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'do_reports_compile.cmd' ); 
     ELSIF  :PARAMETER.P_HOST_CLIENT='HOST' THEN     
       HOST('CMD /C call ' || :PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'do_reports_compile.cmd' , NO_SCREEN );
     END IF;
     
    g.msg_free;
    
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_run_batch_comp_reports_cmd;
  
  ----------------------------------------------------------------------------------------------------------------------------
  --- ACHTUNG: SOURCE-DIR := UNIX-DIR !!! <<<<<<<<<<<<<<<<<<<
  -----------------------------------------------------------
  PROCEDURE prc_run_batch_compile_cmd (p_srcfile VARCHAR2) IS
    -- p_batch : dummy .
  BEGIN
    g.msg_push('pkg_FAM_run_Script.prc_run_batch_compile_cmd');  
      
    -- save actual source file --
    :PARAMETER.P_SOURCE_FILE:= p_srcfile;
    
    ------------------- BO - write BATCH ---------------------
    m_BATCH_COMPILE_run:= C_BATCH_COMPILE_def;

    m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_source_network}',             substr(:PARAMETER.P_ADDON_DIR,1,2));      
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_config_formsapi_execute}',   :PARAMETER.P_CONFIG_FORMSAPI_EXECUTE);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_source_dir}',                :PARAMETER.P_UNIX_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_addon_dir}',                :PARAMETER.P_ADDON_DIR);
    m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_srcfile}',                   p_srcfile);
    m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_unix_dir}',                 :PARAMETER.P_UNIX_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_script_dir}',                :PARAMETER.P_SCRIPT_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_db_connect}',                :PARAMETER.P_DB_CONNECT);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_comp_dir}',                  :PARAMETER.P_COMP_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_log_dir}',                  :PARAMETER.P_LOG_DIR);
     m_BATCH_COMPILE_run:= replace(m_BATCH_COMPILE_run, '${p_log_file}',                  p_srcfile||'-compile.txt');

     prc_wrt_CMD('do_compile.cmd', m_BATCH_COMPILE_run);
     
     SYNCHRONIZE;
      
     ------------------- EO - write BATCH ---------------------
      
     prc_iflog('--- BO BATCH-COMPILE : '||  m_BATCH_COMPILE_run);

    -- 22.03.2017 -- 
    IF :PARAMETER.P_HOST_CLIENT='CLIENT' THEN      
        WEBUTIL_HOST.BLOCKING( 'CMD /C call ' || :PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'do_compile.cmd' );    
     ELSIF :PARAMETER.P_HOST_CLIENT='HOST' THEN   
       HOST( 'CALL '||:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'do_compile.cmd' , NO_SCREEN );    
     END IF;
     
    g.msg_free;
    
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_run_batch_compile_cmd;
  
  ----------------------------------------------------------------------------------------------------------------------------
  PROCEDURE prc_run_FAM_check_script(p_srcfile VARCHAR2) IS 
  BEGIN
     g.msg_push('pkg_run_FAM_script.prc_run_FAM_check_script');
     
     -- save actual source file --
     :PARAMETER.P_SOURCE_FILE:= p_srcfile;
       
     ------------------- BO - write SCRIPT ---------------------
     m_FAM_SCRIPT_chkref:= C_FAM_SCRIPT_chkref;
     m_FAM_SCRIPT_chkref:= replace(C_FAM_SCRIPT_chkref, '\''', '''');
     m_FAM_SCRIPT_chkref:= replace(m_FAM_SCRIPT_chkref, '${p_source_dir}',:PARAMETER.P_SOURCE_DIR);
     m_FAM_SCRIPT_chkref:= replace(m_FAM_SCRIPT_chkref, '${p_srcfile}',p_srcfile);
      
     prc_wrt_CMD('check-ref.p2s', m_FAM_SCRIPT_chkref);    
     ------------------- EO - write SCRIPT ---------------------
     
     ------------------- BO - write CMD ------------------------
     m_CMD_RUN_chkref:= C_CMD_RUN_SCRIPT_chkref;
     --- 1.) set FORMS_PATH ! then check references ! --   
     m_CMD_RUN_chkref:= replace(m_CMD_RUN_chkref, '${p_source_dir}',:PARAMETER.P_SOURCE_DIR);
     m_CMD_RUN_chkref:= replace(m_CMD_RUN_chkref, '${p_addon_dir}',:PARAMETER.P_ADDON_DIR);
     m_CMD_RUN_chkref:= replace(m_CMD_RUN_chkref, '${p_srcfile}',p_srcfile);
     -- 2.) set & write CMD --
     m_CMD_RUN_chkref:= replace(m_CMD_RUN_chkref, '${p_config_formsapi_execute}', :PARAMETER.P_CONFIG_FORMSAPI_EXECUTE);
     m_CMD_RUN_chkref:= replace(m_CMD_RUN_chkref, '${p_script_dir}',              :PARAMETER.P_SCRIPT_DIR);
     m_CMD_RUN_chkref:= replace(m_CMD_RUN_chkref, '${p_script_name}',              'check-ref.p2s');
     
     --prc_info(m_CMD_RUN_chkref);
     prc_wrt_CMD('check-ref.cmd', m_CMD_RUN_chkref);
     SYNCHRONIZE;
     
     ------------------- EO - write CMD ------------------------
          
     -- 3.) run CMD         --
     prc_iflog('--- BO SCRIPT-CHECK-REF : '||m_CMD_RUN_chkref);
     
     -- 22.03.2017 /26.04.2017 -- 
     IF :PARAMETER.P_HOST_CLIENT='CLIENT' THEN 
         WEBUTIL_HOST.BLOCKING( 'CMD /C CALL '||:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'check-ref.cmd' ); 
      ELSIF  :PARAMETER.P_HOST_CLIENT='HOST' THEN 
        HOST( 'CMD /C CALL '||:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'check-ref.cmd' , NO_SCREEN );
      END IF;
      
      prc_iflog('--- EO SCRIPT-CHECK-REF --- ');
      
     g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_run_FAM_check_script;

  ----------------------------------------------------------------------------------------------------------------------------
  PROCEDURE prc_run_FAM_script_mig(p_script_name VARCHAR2) IS 
  BEGIN
     g.msg_push('pkg_run_FAM_script.prc_run_FAM_script_mig');
     
     m_CMD_RUN_SCRIPT:= C_CMD_RUN_SCRIPT_def_mig;
     
     -- 1.) set FORMS_PATH  --
     m_CMD_RUN_SCRIPT:=replace(m_CMD_RUN_SCRIPT,  '${p_addon_dir}',        :PARAMETER.P_ADDON_DIR);
     m_CMD_RUN_SCRIPT:= replace(m_CMD_RUN_SCRIPT, '${p_source_dir}',       :PARAMETER.P_SOURCE_DIR);
     m_CMD_RUN_SCRIPT:= replace(m_CMD_RUN_SCRIPT, '${p_source_network}',  substr(:PARAMETER.P_ADDON_DIR,1,2));
     
     -- 2.) set & write CMD --
     m_CMD_RUN_SCRIPT:= replace(m_CMD_RUN_SCRIPT, '${p_config_formsapi_execute}', :PARAMETER.P_CONFIG_FORMSAPI_EXECUTE);
     m_CMD_RUN_SCRIPT:= replace(m_CMD_RUN_SCRIPT, '${p_script_dir}',              :PARAMETER.P_SCRIPT_DIR);
     m_CMD_RUN_SCRIPT:= replace(m_CMD_RUN_SCRIPT, '${p_script_name}',              p_script_name);
          
     prc_wrt_CMD('run_migration_one.cmd', m_CMD_RUN_SCRIPT);
     
     SYNCHRONIZE;
     
     -- 3.) run CMD         --
     prc_iflog('--- BO SCRIPT-MIGRATE-ONE : '||  m_CMD_RUN_SCRIPT);
  
     -- 22.03.2017 /26.04.2017 -- 
     IF :PARAMETER.P_HOST_CLIENT='CLIENT' THEN 
         WEBUTIL_HOST.BLOCKING( 'CMD /C CALL '||:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'run_migration_one.cmd' );
      ELSIF  :PARAMETER.P_HOST_CLIENT='HOST' THEN 
        HOST( 'CMD /C CALL '||:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'run_migration_one.cmd' , NO_SCREEN );
      END IF;  
      
      prc_iflog('--- EO SCRIPT-MIGRATE-ONE --- ');
      
     g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_run_FAM_script_mig;
  
  ----------------------------------------------------------------------------------------------------------------------------  
  PROCEDURE prc_run_FAM_script_win2unix(p_script_name VARCHAR2) IS 
  BEGIN
     g.msg_push('pkg_run_FAM_script.prc_run_FAM_script_w2unix');
     
     m_CMD_RUN_SCRIPT:= C_CMD_RUN_SCRIPT_def_win2unix;
     
     -- 1.) set FORMS_PATH  --
     m_CMD_RUN_SCRIPT:=replace(m_CMD_RUN_SCRIPT,  '${p_addon_dir}',        :PARAMETER.P_ADDON_DIR);
     m_CMD_RUN_SCRIPT:= replace(m_CMD_RUN_SCRIPT, '${p_source_dir}',       :PARAMETER.P_SOURCE_DIR);
     m_CMD_RUN_SCRIPT:= replace(m_CMD_RUN_SCRIPT, '${p_source_network}',  substr(:PARAMETER.P_ADDON_DIR,1,2));
     
     -- 2.) set & write CMD --
     m_CMD_RUN_SCRIPT:= replace(m_CMD_RUN_SCRIPT, '${p_config_formsapi_execute}', :PARAMETER.P_CONFIG_FORMSAPI_EXECUTE);
     m_CMD_RUN_SCRIPT:= replace(m_CMD_RUN_SCRIPT, '${p_script_dir}',              :PARAMETER.P_SCRIPT_DIR);
     m_CMD_RUN_SCRIPT:= replace(m_CMD_RUN_SCRIPT, '${p_script_name}',              p_script_name);
          
     prc_wrt_CMD('run_win2unix.cmd', m_CMD_RUN_SCRIPT);
     
     SYNCHRONIZE;
     
     -- 3.) run CMD         --
     prc_iflog('--- BO SCRIPT-MIGRATE-ONE : '||  m_CMD_RUN_SCRIPT);
  
     -- 22.03.2017 /26.04.2017 -- 
     IF :PARAMETER.P_HOST_CLIENT='CLIENT' THEN 
         WEBUTIL_HOST.BLOCKING( 'CMD /C CALL '||:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'run_win2unix.cmd' );
      ELSIF  :PARAMETER.P_HOST_CLIENT='HOST' THEN 
        HOST( 'CMD /C CALL '||:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'run_win2unix.cmd' , NO_SCREEN );
      END IF;
      
      prc_iflog('--- EO SCRIPT-MIGRATE-ONE --- ');
      
     g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_run_FAM_script_win2unix;
  
  ---------------------------------------------------------------------------------------------------------------------------------
  
END pkg_FAM_run_Script;