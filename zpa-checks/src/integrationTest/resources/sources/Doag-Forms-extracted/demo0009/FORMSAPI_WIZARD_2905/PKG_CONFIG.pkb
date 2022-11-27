PACKAGE BODY pkg_CONFIG IS

  /* Konfigurierungen - Extern - INI  & Intern.
   * Erstellt: 24.01.2017 , 28.04.2017 - F.Matz
   *
   */
   
  m_CONFIG_DEF CONSTANT VARCHAR2(4096):= '
  [main]

/** Datenbankverbindung wurde direkt ueber USERID im Script zugeordnet
/** db_bvi=statistik_fr/STATISTIK_FR@cds
/** db_connect=test/test@cds
db_connect=${p_db_connect}
one_fmb=${p_source_file}

/** Fuer die verschiedenen Komponenten z.B. colorcoordmig
/** color => Farbpaletten
/** coord => Koordinatensystemanpassung
/** mig => Standard Migration
/** resize => Die Groesse der Forms kann geaendert werden
/** win2unix => Die Windows Forms werden zu Unix konvertiert

mig_steps=mig

/** Welche Module werden bearbeitet z.B. pllmmbfmb
/** mmb => Menues
/** pll => Bibliotheken
/** fmb => Forms

mig_modules=pllmmbfmb

/** skriptdir=C:\oracle\migration\karibu\batch\skripte
skriptdir=${p_script_dir}
/** source_dir=C:\oracle\migration\karibu\batch\01source
source_dir=${p_source_dir}
/** color_dir=C:\oracle\migration\karibu\batch\02color
color_dir=${p_color_dir}
/** coord_dir=C:\oracle\migration\karibu\batch\03coord
coord_dir=${p_coord_dir}
/** mig_dir=C:\oracle\migration\karibu\batch\04migrated
mig_dir=${p_migr_dir}
/** unix_dir=C:\oracle\migration\karibu\batch\05unix
unix_dir=${p_unix_dir}

/** Umschalten zwischen lower und upper case
/** lower => alles klein geschrieben
/** upper => alles gross geschrieben
mig_unixcase=lower

[webutil]
pll=webutil.pll
/** olb=C:\oracle\migration\karibu\batch\00addon\olb_webutil.olb
olb=${p_addon_dir}\${p_olb_file}

[runproduct]
pll=mig_print_lib.pll

[Client_util]
pll=CLIENT_UTIL.pll

[error]
errorlog=error.log

[reportdummy]
/** Bei verwendung der mig_print_lib.pll muss diese auf das Reportobject angepasst werden
reportObject=dummy

[coordsystem]
targetcoord=real
realcoordunit=pixel
defaultvalues=true
cellwidth=9
cellhight=21

[Comment]
showMigPLSQLComment=TRUE
';
  
  m_CONFIG_INI_RDY VARCHAR2(4096);
  m_CONFIG_INI_RUN VARCHAR2(4096);
  m_log            CONSTANT BOOLEAN := FALSE;
  
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
  PROCEDURE prc_wrt_Config_INI IS
     l_io_file    Text_IO.File_Type;
     l_io_file_C  client_Text_IO.File_Type;
  BEGIN
     g.msg_push('pkg_CONFIG.prc_wrt_Config_INI');
     
     IF :PARAMETER.P_HOST_CLIENT='HOST' THEN 
         SYNCHRONIZE;
        l_io_file := Text_IO.Fopen(:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'config.ini', 'w');
        text_io.put_line(l_io_file, m_CONFIG_INI_RUN);                                                                         
        text_io.fclose  (l_io_file);
        SYNCHRONIZE;
        
     ELSIF :PARAMETER.P_HOST_CLIENT='CLIENT' THEN 
        SYNCHRONIZE;
        l_io_file_C := client_Text_IO.Fopen(:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'config.ini', 'w');
        client_Text_IO.put_line(l_io_file_C, m_CONFIG_INI_RUN);                                                                         
        client_Text_IO.fclose  (l_io_file_C);
        SYNCHRONIZE;
        
     END IF;
     
      g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_wrt_Config_INI;
  
-------------------------------------------------------------------------------------------------------
  -- 28.04.2017 --
  PROCEDURE prc_wrt_CMD(p_cmd_name VARCHAR2, p_script VARCHAR2) IS
     l_io_file    Text_IO.File_Type;
     l_io_file_C  client_Text_IO.File_Type;
  BEGIN
     g.msg_push('pkg_CONFIG.prc_wrt_CMD');
     
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
  PROCEDURE prc_set_Config_INI IS  
  BEGIN
     g.msg_push('pkg_CONFIG.prc_Config_INI');
     
     m_CONFIG_INI_RDY:= m_CONFIG_DEF;
     m_CONFIG_INI_RDY:= replace(m_CONFIG_INI_RDY, '${p_db_connect}',   :PARAMETER.P_DB_CONNECT);
     m_CONFIG_INI_RDY:= replace(m_CONFIG_INI_RDY, '${p_script_dir}',   :PARAMETER.P_SCRIPT_DIR);
     m_CONFIG_INI_RDY:= replace(m_CONFIG_INI_RDY, '${p_source_dir}',   :PARAMETER.P_SOURCE_DIR);
      m_CONFIG_INI_RDY:= replace(m_CONFIG_INI_RDY, '${p_addon_dir}',   :PARAMETER.P_ADDON_DIR);
     m_CONFIG_INI_RDY:= replace(m_CONFIG_INI_RDY, '${p_color_dir}',   :PARAMETER.P_COLOR_DIR);
     m_CONFIG_INI_RDY:= replace(m_CONFIG_INI_RDY, '${p_coord_dir}',   :PARAMETER.P_COORD_DIR);
     m_CONFIG_INI_RDY:= replace(m_CONFIG_INI_RDY, '${p_migr_dir}',     :PARAMETER.P_MIGR_DIR);
     m_CONFIG_INI_RDY:= replace(m_CONFIG_INI_RDY, '${p_unix_dir}',     :PARAMETER.P_UNIX_DIR);
     m_CONFIG_INI_RDY:= replace(m_CONFIG_INI_RDY, '${p_olb_file}',     :PARAMETER.P_OLB_FILE);
     
     -- for tests --
     -- prc_wrt_Config_INI;
     g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_set_Config_INI;
  
-------------------------------------------------------------------------------------------------------   
  PROCEDURE prc_set_Config_INI_Source(p_srcfile VARCHAR2) IS
  BEGIN
     g.msg_push('pkg_CONFIG.prc_Config_INI_Source');
     
     prc_set_Config_INI;
     m_CONFIG_INI_RUN:= m_CONFIG_INI_RDY;
     
     :PARAMETER.P_SOURCE_FILE:= p_srcfile;
     
     m_CONFIG_INI_RUN:= replace(m_CONFIG_INI_RUN, '${p_source_file}', :PARAMETER.P_SOURCE_FILE);
     
     prc_wrt_Config_INI;
     
     g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_set_Config_INI_Source;
  
-------------------------------------------------------------------------------------------------------   
  PROCEDURE prc_create_DIRS IS
  BEGIN
     g.msg_push('pkg_Config.prc_create_DIRS');
     
     :PARAMETER.P_DB_CONNECT := :PARAMETER.P_DB_CONNECT;
     
      -- set PARAMETERS final --
      :PARAMETER.P_WORKSPACE  := :PARAMETER.P_CONFIG_HOME||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_PLACE;
      :PARAMETER.P_SCRIPT_DIR := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_SCRIPT;
      :PARAMETER.P_LOG_DIR    := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_LOG;
      :PARAMETER.P_SOURCE_DIR := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_SOURCE;
      :PARAMETER.P_ADDON_DIR  := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_ADDON;
      :PARAMETER.P_COLOR_DIR  := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_COLOR;
      :PARAMETER.P_COORD_DIR  := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_COORD;
      :PARAMETER.P_MIGR_DIR   := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_MIGR;
      :PARAMETER.P_UNIX_DIR   := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_UNIX;   
      :PARAMETER.P_COMP_DIR   := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_COMP;      
      :PARAMETER.P_OLB_FILE   := :PARAMETER.P_CONFIG_OLB_FILE;
                    
     WEBUTIL_HOST.HOST( 'CMD /C IF NOT EXIST '||:PARAMETER.P_SCRIPT_DIR ||' MKDIR '||:PARAMETER.P_SCRIPT_DIR);  
     WEBUTIL_HOST.HOST( 'CMD /C IF NOT EXIST '||:PARAMETER.P_LOG_DIR     ||' MKDIR '||:PARAMETER.P_LOG_DIR);          
     WEBUTIL_HOST.HOST( 'CMD /C IF NOT EXIST '||:PARAMETER.P_SOURCE_DIR ||' MKDIR '||:PARAMETER.P_SOURCE_DIR);  
     WEBUTIL_HOST.HOST( 'CMD /C IF NOT EXIST '||:PARAMETER.P_ADDON_DIR  ||' MKDIR '||:PARAMETER.P_ADDON_DIR);            
     WEBUTIL_HOST.HOST( 'CMD /C IF NOT EXIST '||:PARAMETER.P_COLOR_DIR  ||' MKDIR '||:PARAMETER.P_COLOR_DIR);            
     WEBUTIL_HOST.HOST( 'CMD /C IF NOT EXIST '||:PARAMETER.P_COORD_DIR  ||' MKDIR '||:PARAMETER.P_COORD_DIR);            
     WEBUTIL_HOST.HOST( 'CMD /C IF NOT EXIST '||:PARAMETER.P_MIGR_DIR   ||' MKDIR '||:PARAMETER.P_MIGR_DIR);            
     WEBUTIL_HOST.HOST( 'CMD /C IF NOT EXIST '||:PARAMETER.P_UNIX_DIR   ||' MKDIR '||:PARAMETER.P_UNIX_DIR);            
     WEBUTIL_HOST.HOST( 'CMD /C IF NOT EXIST '||:PARAMETER.P_COMP_DIR   ||' MKDIR '||:PARAMETER.P_COMP_DIR);     

     WEBUTIL_HOST.HOST( 'CMD /C DEL  '||:PARAMETER.P_LOG_DIR     ||'\*.* /Q');      
     WEBUTIL_HOST.HOST( 'CMD /C DEL  '||:PARAMETER.P_SOURCE_DIR ||'\*.* /Q');  
     WEBUTIL_HOST.HOST( 'CMD /C DEL  '||:PARAMETER.P_ADDON_DIR  ||'\*.* /Q');          
     WEBUTIL_HOST.HOST( 'CMD /C DEL  '||:PARAMETER.P_COLOR_DIR  ||'\*.* /Q');          
     WEBUTIL_HOST.HOST( 'CMD /C DEL  '||:PARAMETER.P_COORD_DIR  ||'\*.* /Q');          
     WEBUTIL_HOST.HOST( 'CMD /C DEL  '||:PARAMETER.P_MIGR_DIR   ||'\*.* /Q');            
     WEBUTIL_HOST.HOST( 'CMD /C DEL  '||:PARAMETER.P_UNIX_DIR   ||'\*.* /Q');            
     WEBUTIL_HOST.HOST( 'CMD /C DEL  '||:PARAMETER.P_COMP_DIR   ||'\*.* /Q');
     
     g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;  
  END prc_create_DIRS;
  
-------------------------------------------------------------------------------------------------------   
  PROCEDURE prc_create_DIRS_HOST IS
  BEGIN
     g.msg_push('pkg_Config.prc_create_DIRS_HOST');
     
     :PARAMETER.P_DB_CONNECT := :PARAMETER.P_DB_CONNECT;
     
      -- set PARAMETERS final --
      :PARAMETER.P_WORKSPACE  := :PARAMETER.P_CONFIG_HOME||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_PLACE;
      :PARAMETER.P_SCRIPT_DIR := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_SCRIPT;
      :PARAMETER.P_LOG_DIR    := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_LOG;
      :PARAMETER.P_SOURCE_DIR := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_SOURCE;
      :PARAMETER.P_ADDON_DIR  := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_ADDON;
      :PARAMETER.P_COLOR_DIR  := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_COLOR;
      :PARAMETER.P_COORD_DIR  := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_COORD;
      :PARAMETER.P_MIGR_DIR   := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_MIGR;
      :PARAMETER.P_UNIX_DIR   := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_UNIX;   
      :PARAMETER.P_COMP_DIR   := :PARAMETER.P_WORKSPACE  ||:PARAMETER.P_FILE_SEPARATOR||:PARAMETER.P_CONFIG_COMP;      
      :PARAMETER.P_OLB_FILE   := :PARAMETER.P_CONFIG_OLB_FILE;
                    
     -- HOST( 'start CMD /C IF NOT EXIST '||:PARAMETER.P_SCRIPT_DIR ||' MKDIR '||:PARAMETER.P_SCRIPT_DIR,  NO_SCREEN);  
     HOST( 'IF NOT EXIST '||:PARAMETER.P_SCRIPT_DIR ||' MKDIR '||:PARAMETER.P_SCRIPT_DIR,  NO_SCREEN);  
     HOST( 'IF NOT EXIST '||:PARAMETER.P_LOG_DIR     ||' MKDIR '||:PARAMETER.P_LOG_DIR,    NO_SCREEN);          
     HOST( 'IF NOT EXIST '||:PARAMETER.P_SOURCE_DIR ||' MKDIR '||:PARAMETER.P_SOURCE_DIR,  NO_SCREEN);  
     HOST( 'IF NOT EXIST '||:PARAMETER.P_ADDON_DIR  ||' MKDIR '||:PARAMETER.P_ADDON_DIR,  NO_SCREEN);            
     HOST( 'IF NOT EXIST '||:PARAMETER.P_COLOR_DIR  ||' MKDIR '||:PARAMETER.P_COLOR_DIR,  NO_SCREEN);            
     HOST( 'IF NOT EXIST '||:PARAMETER.P_COORD_DIR  ||' MKDIR '||:PARAMETER.P_COORD_DIR,  NO_SCREEN);            
     HOST( 'IF NOT EXIST '||:PARAMETER.P_MIGR_DIR   ||' MKDIR '||:PARAMETER.P_MIGR_DIR,    NO_SCREEN);            
     HOST( 'IF NOT EXIST '||:PARAMETER.P_UNIX_DIR   ||' MKDIR '||:PARAMETER.P_UNIX_DIR,    NO_SCREEN);            
     HOST( 'IF NOT EXIST '||:PARAMETER.P_COMP_DIR   ||' MKDIR '||:PARAMETER.P_COMP_DIR,    NO_SCREEN);     

     HOST( 'DEL  '||:PARAMETER.P_LOG_DIR     ||'\*.* /Q',  NO_SCREEN);      
     HOST( 'DEL  '||:PARAMETER.P_SOURCE_DIR ||'\*.* /Q',    NO_SCREEN);  
     HOST( 'DEL  '||:PARAMETER.P_ADDON_DIR  ||'\*.* /Q',    NO_SCREEN);          
     HOST( 'DEL  '||:PARAMETER.P_COLOR_DIR  ||'\*.* /Q',    NO_SCREEN);          
     HOST( 'DEL  '||:PARAMETER.P_COORD_DIR  ||'\*.* /Q',    NO_SCREEN);          
     HOST( 'DEL  '||:PARAMETER.P_MIGR_DIR   ||'\*.* /Q',    NO_SCREEN);            
     HOST( 'DEL  '||:PARAMETER.P_UNIX_DIR   ||'\*.* /Q',    NO_SCREEN);            
     HOST( 'DEL  '||:PARAMETER.P_COMP_DIR   ||'\*.* /Q',    NO_SCREEN);
     
     g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;  
  END prc_create_DIRS_HOST;
  
------------------------------------------------------------------------------------------------------- 
  PROCEDURE prc_Copy_Dir(p_srcdir VARCHAR2, p_destdir VARCHAR2) IS
  BEGIN
     g.msg_push('pkg_Config.prc_Copy_Dir');
     
     prc_iflog('--- COPY :: '||'CMD /C XCOPY '|| p_srcdir ||' '|| p_destdir ||' /S /D /V /Y'||' .');
     
     WEBUTIL_HOST.BLOCKING('CMD /C XCOPY '|| p_srcdir ||' '|| p_destdir ||' /S /D /V /Y');

     g.msg_free;
  
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_Copy_Dir;

------------------------------------------------------------------------------------------------------- 
  PROCEDURE prc_Copy_Dir_HOST(p_srcdir VARCHAR2, p_destdir VARCHAR2) IS
  BEGIN    
     g.msg_push('pkg_Config.prc_Copy_Dir_HOST');
     
     prc_iflog('--- COPY :: '||'XCOPY '|| p_srcdir ||' '|| p_destdir ||' /S /D /V /Y'||' .');
     
      prc_wrt_CMD('do_XCOPY.cmd', :PARAMETER.P_WIN_SYSTEM32_HOME||'\'||'XCOPY '|| p_srcdir ||' '|| p_destdir ||' /S /D /V /Y');
      
      HOST('call ' || :PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'do_XCOPY.cmd', NO_SCREEN);
  
     g.msg_free;
  
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_Copy_Dir_HOST;

------------------------------------------------------------------------------------------------------- 
  PROCEDURE prc_Copy_Dir_RDF(p_srcdir VARCHAR2, p_destdir VARCHAR2) IS
  BEGIN
     g.msg_push('pkg_Config.prc_Copy_Dir');
     
     prc_iflog('--- COPY :: '||'CMD /C XCOPY '|| p_srcdir ||' '|| p_destdir ||' *.RDF /S /D /V /Y'||' .');
     
     WEBUTIL_HOST.BLOCKING('CMD /C XCOPY '|| p_srcdir ||' '|| p_destdir ||' *.RDF /S /D /V /Y');

     g.msg_free;
  
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_Copy_Dir_RDF;

------------------------------------------------------------------------------------------------------- 
  PROCEDURE prc_Copy_Dir_RDF_HOST(p_srcdir VARCHAR2, p_destdir VARCHAR2) IS
  BEGIN
     g.msg_push('pkg_Config.prc_Copy_Dir_HOST');
     
     prc_iflog('--- COPY :: '||'XCOPY '|| p_srcdir ||' '|| p_destdir ||' *.RDF /S /D /V /Y'||' .');
     
      prc_wrt_CMD('do_XCOPY_rdf.cmd', :PARAMETER.P_WIN_SYSTEM32_HOME||'\'||'XCOPY '|| p_srcdir ||' '|| p_destdir ||' *.RDF /S /D /V /Y');
      
      HOST('call ' || :PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'do_XCOPY_rdf.cmd', NO_SCREEN);
      
     -- HOST('XCOPY '|| p_srcdir ||' '|| p_destdir ||' *.RDF /S /D /V /Y', NO_SCREEN);

     g.msg_free;
  
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_Copy_Dir_RDF_HOST;
  
-------------------------------------------------------------------------------------------------------   
  PROCEDURE prc_get_config(p_fname VARCHAR2) IS
       /* 
        * Erstellt: 04.01.2017 - F.Matz
        */
        
       l_fn VARCHAR2(512):= p_fname;
       
       TYPE t_var IS VARRAY(4096) OF VARCHAR2(1); 
       ar  t_var:= t_var();
       ar2 t_var:= t_var();
       
       C_MAX_LEN   CONSTANT NUMBER(4):= 4096;
       l_buff     VARCHAR2(4096);
       
       l_in_file  Text_IO.File_Type;
       l_cnt       NUMBER(3);
       l_pos       NUMBER(3);
       l_i         NUMBER(3);
       l_res       VARCHAR2(128);
       l_section   VARCHAR2(128);
       l_key       VARCHAR2(128);
       l_value     VARCHAR2(2048);
       
   PROCEDURE swap IS
   BEGIN
       ar:= t_var();  -- init --
       FOR i IN 1..length(l_buff) LOOP
            ar.extend(); -- Extend it
            ar(i):=substr(l_buff, i, 1);
       END LOOP;       
   END swap;
    
   FUNCTION wsp RETURN VARCHAR2 IS
   BEGIN  
       FOR i IN 1..ar.COUNT LOOP   
            IF ar(i)='#' THEN
                RETURN('#');
            ELSIF ar(i) IN (' ', ',', ';', CHR(9), CHR(10), CHR(13)) THEN
                NULL;
            ELSIF ar(i) IN ('[') THEN
                RETURN('SECTION');
            ELSE
                RETURN('TOKE');
            END IF;
            l_pos:= l_pos+1;
       END LOOP;
       RETURN(' ');
   END;
    
   FUNCTION   section RETURN VARCHAR2 IS
       l_section VARCHAR2(128);
   BEGIN
       l_pos:= l_pos+1;
       FOR i IN l_pos..ar.COUNT LOOP
            IF ar(i)=']' THEN
                RETURN(l_section);
            END IF;
            l_section:= l_section||ar(i);
            l_pos:= l_pos+1;
       END LOOP;     
   END section;
    
   PROCEDURE keyvalue(p_out_key OUT VARCHAR2, p_out_value OUT VARCHAR2) IS
   BEGIN
       -- pos at FIRST --
       FOR i IN l_pos..ar.COUNT LOOP
            IF ar(i) IN (' ', CHR(9), '=') THEN 
                EXIT;  -- key --
            ELSE
                p_out_key:= p_out_key||ar(i);
            END IF;
            l_pos:= l_pos+1;
       END LOOP;
       
       -- go over --
       FOR i IN l_pos..ar.COUNT LOOP
            IF ar(i) IN (' ', CHR(9), '=') THEN
                NULL;
            ELSE
                EXIT; -- first --
            END IF;
           l_pos:= l_pos+1;
       END LOOP;
          
       -- pos at FIRST --
       FOR i IN l_pos..ar.COUNT LOOP
            IF ar(i) IN (' ', ',', ';', CHR(9), CHR(10), CHR(13)) THEN 
                EXIT;  -- value --
            ELSE
                p_out_value:= p_out_value||ar(i);
            END IF;
            l_pos:= l_pos+1;
       END LOOP;
       
   END keyvalue;
    
   BEGIN
         l_buff:='';
        l_in_file := Text_IO.Fopen(l_fn, 'r');
        l_cnt:=1;
        LOOP
           BEGIN
              Text_IO.get_line(l_in_File, l_buff);
               l_pos:=1;
               IF length(l_buff)>0 THEN 
                  swap; 
                  l_res:= wsp;
                  IF l_res='SECTION' THEN
                      l_section:= section;
                      --prc_info(l_section);
                  ELSIF l_res='TOKE' THEN
                      keyvalue(l_key, l_value);
                      --prc_info(l_key||' : '||l_value);
                      IF l_section='DATABASE' THEN
                         CASE l_key
                             WHEN 'db.connect'        THEN :PARAMETER.P_DB_CONNECT               := l_value;
                         END CASE;
                      END IF;
                      IF l_section='PATH' THEN
                         CASE l_key
                             WHEN 'formsapi.execute'  THEN :PARAMETER.P_CONFIG_FORMSAPI_EXECUTE  := l_value;
                             WHEN 'formsapi.scripts'  THEN :PARAMETER.P_CONFIG_FORMSAPI_SCRIPTS  := l_value;
                             WHEN 'user.sources_dir'  THEN :PARAMETER.P_CONFIG_USER_SOURCES      := l_value;
                             WHEN 'user.addons_dir'   THEN :PARAMETER.P_CONFIG_USER_ADDONS       := l_value;
                             WHEN 'user.olb'          THEN :PARAMETER.P_CONFIG_OLB_FILE          := l_value;
                             WHEN 'workspace.home'    THEN :PARAMETER.P_CONFIG_HOME               := l_value;
                             WHEN 'workspace.place'   THEN :PARAMETER.P_CONFIG_PLACE              := l_value;
                         END CASE;
                      END IF;
                  END IF;
               END IF;
               l_cnt:= l_cnt+1;           
           EXCEPTION 
               WHEN NO_DATA_FOUND THEN
                Text_IO.Fclose(l_in_File);
                 EXIT;
               WHEN OTHERS THEN
                   Text_IO.Fclose(l_in_File);
                   prc_info(sqlerrm);
                 EXIT;
               EXIT;
           END ;
       END LOOP;
       
       --prc_iflog('--- db.connect:       '|| :PARAMETER.P_DB_CONNECT);
       --prc_iflog('--- formsapi.execute: '|| :PARAMETER.P_CONFIG_FORMSAPI_EXECUTE);
       --prc_iflog('--- formsapi.scripts: '|| :PARAMETER.P_CONFIG_FORMSAPI_SCRIPTS);
       --prc_iflog('--- user.sources_dir: '|| :PARAMETER.P_CONFIG_USER_SOURCES);
       --prc_iflog('--- user.addons_dir:  '|| :PARAMETER.P_CONFIG_USER_ADDONS);
       --prc_iflog('--- user.olb:         '|| :PARAMETER.P_CONFIG_OLB_FILE);
       --prc_iflog('--- workspace.home:   '|| :PARAMETER.P_CONFIG_HOME);
       --prc_iflog('--- workspace.place:  '|| :PARAMETER.P_CONFIG_PLACE);

EXCEPTION WHEN OTHERS THEN
      NULL; -- ignore --
  END prc_get_config;
------------------------------------------------------------------------------------------------------- 

END pkg_CONFIG;