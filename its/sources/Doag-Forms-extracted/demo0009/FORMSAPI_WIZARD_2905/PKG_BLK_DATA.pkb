PACKAGE BODY pkg_blk_data IS
  
  -- F.Matz: 02.02.2017, 20.04.2017, 27.04.2017
  
   C_cmd_read_dir_HOST        CONSTANT VARCHAR2(256):= 'dir ${p_directory} /O:N /A:-D > ${p_script_dir}\dir-list.txt';
   m_run_cmd_read_dir_HOST   VARCHAR2(4098);
   m_log           CONSTANT BOOLEAN := FALSE;
  
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
  -- 27.04.2017 --
  PROCEDURE prc_wrt_CMD(p_cmd_name VARCHAR2, p_script VARCHAR2) IS
     l_io_file    Text_IO.File_Type;
     l_io_file_C  client_Text_IO.File_Type;
  BEGIN
     g.msg_push('pkg_blk_data.prc_wrt_CMD');
     
     IF :PARAMETER.P_HOST_CLIENT='HOST' THEN 
        SYNCHRONIZE;
        l_io_file := Text_IO.Fopen(:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||p_cmd_name, 'w');
        Text_IO.put_line(l_io_file, p_script);                                                                         
        Text_IO.fclose  (l_io_file);
        SYNCHRONIZE;
     
     ELSIF :PARAMETER.P_HOST_CLIENT='CLIENT' THEN 
        SYNCHRONIZE;
        l_io_file_C:= client_Text_IO.Fopen(:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||p_cmd_name, 'w');
        client_Text_IO.put_line(l_io_file_C, p_script);                                                                         
        client_Text_IO.fclose(l_io_file_C);
        SYNCHRONIZE;

     END IF;       
     
      g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_wrt_CMD;

--------------------------------------------------------------------------------------------------------

  PROCEDURE prc_get_from_directory IS
  BEGIN    
    NULL;   
  END prc_get_from_directory;

--------------------------------------------------------------------------------------------------------
  
  PROCEDURE prc_HANDLG_tableid2name (p_id NUMBER) IS
     l_name VARCHAR2(128);
  BEGIN
      g.msg_push('pkg_BLOCK_TABLE.prc_HANDLG_tableid2name');

     l_name:= pkg_BLK_DATA.HANDLG_byid(p_id).FNAME;
     pkg_BLK_DATA.HANDLG_byname(l_name).PNO            := pkg_BLK_DATA.HANDLG_byid(p_id).PNO;
     pkg_BLK_DATA.HANDLG_byname(l_name).RNO            := pkg_BLK_DATA.HANDLG_byid(p_id).RNO;
     pkg_BLK_DATA.HANDLG_byname(l_name).FNAME          := pkg_BLK_DATA.HANDLG_byid(p_id).FNAME;
     pkg_BLK_DATA.HANDLG_byname(l_name).FEXTENT        := pkg_BLK_DATA.HANDLG_byid(p_id).FEXTENT;
     pkg_BLK_DATA.HANDLG_byname(l_name).FSIZE          := pkg_BLK_DATA.HANDLG_byid(p_id).FSIZE;
     pkg_BLK_DATA.HANDLG_byname(l_name).DFILE          := pkg_BLK_DATA.HANDLG_byid(p_id).DFILE;
     pkg_BLK_DATA.HANDLG_byname(l_name).FORMS_OBJ_VNO  := pkg_BLK_DATA.HANDLG_byid(p_id).FORMS_OBJ_VNO;
     pkg_BLK_DATA.HANDLG_byname(l_name).FAM_VERSION    := pkg_BLK_DATA.HANDLG_byid(p_id).FAM_VERSION;
     pkg_BLK_DATA.HANDLG_byname(l_name).FSTATUS        := pkg_BLK_DATA.HANDLG_byid(p_id).FSTATUS;
     pkg_BLK_DATA.HANDLG_byname(l_name).FMESSAGE      := pkg_BLK_DATA.HANDLG_byid(p_id).FMESSAGE;   
           
     pkg_BLK_DATA.HANDLG_byname(l_name).CB            := pkg_BLK_DATA.HANDLG_byid(p_id).CB;
     pkg_BLK_DATA.HANDLG_byname(l_name).STATUS        := pkg_BLK_DATA.HANDLG_byid(p_id).STATUS;
      pkg_BLK_DATA.HANDLG_byname(l_name).TYPE          := pkg_BLK_DATA.HANDLG_byid(p_id).TYPE;
     pkg_BLK_DATA.HANDLG_byname(l_name).OPERAT        := pkg_BLK_DATA.HANDLG_byid(p_id).OPERAT;
     pkg_BLK_DATA.HANDLG_byname(l_name).MSG            := pkg_BLK_DATA.HANDLG_byid(p_id).MSG;
      pkg_BLK_DATA.HANDLG_byname(l_name).RESULT        := pkg_BLK_DATA.HANDLG_byid(p_id).RESULT;
      pkg_BLK_DATA.HANDLG_byname(l_name).SPECIAL        := pkg_BLK_DATA.HANDLG_byid(p_id).SPECIAL;
     pkg_BLK_DATA.HANDLG_byname(l_name).HISTORY        := pkg_BLK_DATA.HANDLG_byid(p_id).HISTORY;
     
     g.msg_free;
     
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_HANDLG_tableid2name;

--------------------------------------------------------------------------------------------------------
 
  PROCEDURE prc_param2table(p_block VARCHAR2, p_pno  VARCHAR2, p_id NUMBER, p_fname VARCHAR2, p_fextend VARCHAR2, p_fsize NUMBER, p_status VARCHAR2,
                            p_user VARCHAR2 ) IS
                              
     l_date          VARCHAR2(32);                      
  BEGIN
     g.msg_push('pkg_BLK_DATA.prc_param2table');
      
     prc_iflog('--- pkg_BLK_DATA.prc_param2table: HANDLG : '||p_fname||' ---', p_type=>'DEBUG');                        
     l_date := webutil_clientinfo.get_date_time;
     pkg_BLK_DATA.HANDLG_byid(p_id).PNO               := :PARAMETER.P_PNO;
     pkg_BLK_DATA.HANDLG_byid(p_id).RNO               := p_id;
     pkg_BLK_DATA.HANDLG_byid(p_id).FNAME             := p_fname;
     pkg_BLK_DATA.HANDLG_byid(p_id).FEXTENT            := p_fextend;
     pkg_BLK_DATA.HANDLG_byid(p_id).FSIZE             := p_fsize;
      pkg_BLK_DATA.HANDLG_byid(p_id).REC_STATUS         := 'I';
     pkg_BLK_DATA.HANDLG_byid(p_id).REC_USER           := :PARAMETER.P_USER;    
      pkg_BLK_DATA.HANDLG_byid(p_id).REC_DREAD          := NULL;     -- marking: INSERT          
     pkg_BLK_DATA.HANDLG_byid(p_id).REC_DUPDATE       := l_date;          
     pkg_BLK_DATA.HANDLG_byid(p_id).REC_DCREATED      := l_date;  
     pkg_BLK_DATA.HANDLG_byid(p_id).CB                := 'Y';  
          
     prc_HANDLG_tableid2name(p_id);
  
     g.msg_free;
      
    EXCEPTION WHEN OTHERS THEN
       prc_EXCEPTION;
    END  prc_param2table;

-------------------------------------------------------------------------------------------------------- 

  PROCEDURE prc_table2block(p_id NUMBER, p_block VARCHAR2) IS
     l_ix NUMBER(4);
  BEGIN
     g.msg_push('pkg_BLOCK_TABLE.prc_table2block');
     
           prc_iflog('--- pkg_BLOCK_TABLE.prc_table2block: HANDLG : '||p_id||' / '||p_block||' ---', p_type=>'DEBUG');  
             COPY(pkg_BLK_DATA.HANDLG_byid(p_id).PNO,             p_block||'.'||'PNO');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).RNO,             p_block||'.'||'RNO');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).FNAME,            p_block||'.'||'FNAME');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).FEXTENT,          p_block||'.'||'FEXTENT');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).FSIZE,            p_block||'.'||'FSIZE');
            COPY(pkg_BLK_DATA.HANDLG_byid(p_id).DFILE,            p_block||'.'||'DFILE');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).FORMS_OBJ_VNO,   p_block||'.'||'FORMS_OBJ_VNO');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).FAM_VERSION,     p_block||'.'||'FAM_VERSION');
            COPY(pkg_BLK_DATA.HANDLG_byid(p_id).FSTATUS,         p_block||'.'||'FSTATUS');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).FMESSAGE,        p_block||'.'||'FMESSAGE');   
           
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).CB,              p_block||'.'||'CB');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).STATUS,          p_block||'.'||'STATUS');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).TYPE,            p_block||'.'||'TYPE');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).OPERAT,          p_block||'.'||'OPERAT');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).MSG,              p_block||'.'||'MSG');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).RESULT,          p_block||'.'||'RESULT');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).SPECIAL,          p_block||'.'||'SPECIAL');
           COPY(pkg_BLK_DATA.HANDLG_byid(p_id).HISTORY,          p_block||'.'||'HISTORY');
  
  END prc_table2block;

--------------------------------------------------------------------------------------------------------
 
  PROCEDURE prc_exec_from_directory(p_data_type VARCHAR2, p_path VARCHAR2, p_fextend VARCHAR2 DEFAULT '*.fmb') IS
     
     l_file_list     webutil_file.file_list;
     l_date          VARCHAR2(32);
     l_temp_text     VARCHAR2(1000);
     l_fname         VARCHAR2(256);
     l_fsize        NUMBER(12);
     l_fextent      VARCHAR2(3);
     l_fselect      BOOLEAN;
     l_rno           NUMBER(4);
     l_cbselcnt      NUMBER(4);
     l_source_dir   VARCHAR2(2048);
     l_data_blk     VARCHAR2(32):= 'BLK_HANDLG';
     --l_data_ix      NUMBER(2);
     
  BEGIN
     g.msg_push('pkg_BLK_DATA.prc_exec_from_directory');
     
     l_source_dir := p_path;
      
     l_file_list := webutil_file.directory_filtered_list(l_source_dir, '*.*', TRUE);
    
     go_block( l_data_blk );
     clear_block;
    
     first_record;
     
      -- INSERT - loop : New data's ! --
       
     l_rno:=1;
    
     prc_iflog('--- pkg_BLK_DATA.prc_exec_from_directory ---');
     
     FOR i IN 1 .. l_file_list.count
     LOOP
       
        l_fname  :=   l_file_list(i);
        
        -- FH 4.3.2017
        if   instr(lower(l_fname),'.rdf',1,1) > 0
          or instr(lower(l_fname),'.fmb',1,1) > 0 
          or instr(lower(l_fname),'.pll',1,1) > 0
          or instr(lower(l_fname),'.mmb',1,1) > 0 
         then  
          
           l_fextent  :=  lower(substr(l_fname, instr(l_fname,'.')+1, 3));
           l_fsize    :=  replace(webutil_file.file_size(l_source_dir||:PARAMETER.P_FILE_SEPARATOR||l_fname), ',', '.');
                
            prc_param2table(  l_data_blk,
                           :PARAMETER.P_PNO,
                          l_rno,
                          l_fname,
                          UPPER(l_fextent),
                           l_fsize,
                          'I',
                          :PARAMETER.P_USER );
           
           prc_table2block(l_rno, l_data_blk);
        end if;  
              
        l_rno:= l_rno+1;      
      
        IF i<l_file_list.count THEN
          IF NAME_IN(l_data_blk||'.'||'RNO') IS NOT NULL THEN
              next_record;   
           END IF;
        END IF;  
        
    END LOOP;
                
    go_block(l_data_blk);
    first_record;
    
    g.msg_free;
      
  EXCEPTION WHEN OTHERS THEN
     prc_EXCEPTION;
  END prc_exec_from_directory;
    
--------------------------------------------------------------------------------------------------------

  PROCEDURE prc_exec_from_directory_HOST(p_directory VARCHAR2) IS
   
    in_file       Text_IO.File_Type;
    linebuf       VARCHAR2(2048);
    l_fsize       VARCHAR2(32);
    l_fname       VARCHAR2(256);
    l_fextent      VARCHAR2(3);
    l_rno         NUMBER(4);
    l_data_blk     VARCHAR2(32):= 'BLK_HANDLG';
  BEGIN
    
    g.msg_push('pkg_BLK_DATA.prc_exec_from_directory_HOST');
    
    -- get dir list --
    m_run_cmd_read_dir_HOST:= C_cmd_read_dir_HOST;
    m_run_cmd_read_dir_HOST:= replace(m_run_cmd_read_dir_HOST, '${p_directory}',   p_directory);
    m_run_cmd_read_dir_HOST:= replace(m_run_cmd_read_dir_HOST, '${p_script_dir}', :PARAMETER.P_SCRIPT_DIR);
    
    prc_wrt_CMD('do_read_dir.cmd', m_run_cmd_read_dir_HOST);
     
    HOST(:PARAMETER.P_SCRIPT_DIR||:PARAMETER.P_FILE_SEPARATOR||'do_read_dir.cmd', NO_SCREEN);
       
    go_block( l_data_blk );
    clear_block;
    
    first_record;
    
    l_rno:=1;
    
    BEGIN
      in_file := Text_IO.Fopen(:PARAMETER.P_SCRIPT_DIR||'\'||'dir-list.txt', 'r');  
     
      LOOP
         Text_IO.Get_Line(in_file, linebuf);
         --Text_IO.New_Line;
         
         IF substr(linebuf,1,1) IN ('0','1','2','3','4','5','6','7','8','9') THEN
            
             -- get size / fname / fextent --
             l_fsize  := replace(ltrim(substr(linebuf, 18, 18)),'.');        
             l_fname  := substr(linebuf, 37, length(linebuf)-37+1);
             l_fextent:= UPPER(substr(l_fname, instr(l_fname,'.')+1, 3));
             
             IF l_fextent IN ('FMB', 'PLL', 'MMB', 'RDF') THEN     
                prc_param2table( l_data_blk,
                                 :PARAMETER.P_PNO,
                                 l_rno,
                                l_fname,
                                l_fextent,
                                 l_fsize,
                                'I',
                                :PARAMETER.P_USER );  
               prc_table2block(l_rno, l_data_blk);                        
               l_rno:= l_rno+1;      
                next_record; 
             END IF;            
         END IF;        
      END LOOP;
    
    EXCEPTION
    WHEN no_data_found THEN
         Text_IO.Fclose(in_file);
    END;

    go_block(l_data_blk);
    first_record;
      
    g.msg_free;
      
  EXCEPTION WHEN OTHERS THEN 
     prc_EXCEPTION;
  END prc_exec_from_directory_HOST;
  
END;