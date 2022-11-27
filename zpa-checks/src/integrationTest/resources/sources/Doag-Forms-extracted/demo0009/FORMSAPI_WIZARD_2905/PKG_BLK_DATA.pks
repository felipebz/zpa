PACKAGE pkg_blk_data IS
  
  -- F.Matz: 02.02.107, 21.04.2017
   
  C_MAX_RECORDS CONSTANT NUMBER(5):= 1000;  -- max. files per table/block.
  
  TYPE rec_t IS RECORD (
      PNO            VARCHAR2(32)
    ,CB              VARCHAR2(4)
     ,RNO            NUMBER(4)
    ,FNAME          VARCHAR2(512)
    ,FSIZE          NUMBER(12)
    ,FEXTENT        VARCHAR2(5)
    ,DFILE          VARCHAr2(32)
    ,FORMS_OBJ_VNO  VARCHAR2(32)
    ,FAM_VERSION    VARCHAR2(128)
    ,FSTATUS        VARCHAR2(16)
    ,FMESSAGE        VARCHAR2(256)
    ,STATUS          VARCHAR2(16)
    ,STATUS_CHECK    VARCHAR2(16)
    ,STATUS_MIGRATE  VARCHAR2(16)
    ,STATUS_COMPILE  VARCHAR2(16)
    ,TYPE            VARCHAR2(32)
    ,OPERAT          VARCHAR2(64)
    ,MSG            VARCHAR2(256)
    ,RESULT          VARCHAR2(256)
    ,SPECIAL        VARCHAR2(256)
    ,HISTORY        VARCHAR2(2048)
    ,BLOCK_NAME      VARCHAR2(32)
    ,REC_HASH        VARCHAR2(64)
    ,REC_STATUS      VARCHAR2(16)
    ,REC_USER        VARCHAR2(64)
    ,REC_DREAD      VARCHAR2(32)
    ,REC_DUPDATE    VARCHAR2(32)
    ,REC_DCREATED    VARCHAR2(32)
  );
     
  TYPE rec_tabname_t IS TABLE OF rec_t INDEX BY VARCHAR2(128);    -- order by name  
  TYPE rec_tabid_t   IS TABLE OF rec_t INDEX BY PLS_INTEGER;      -- order by id  
  
  -- EO declaration record types for blocks --
 
  -- BO real block tables       --
  HANDLG_byname         rec_tabname_t;
  HANDLG_byid           rec_tabid_t;  
  -- EO real block tables       --
   
  PROCEDURE prc_exec_from_directory(p_data_type VARCHAR2, p_path VARCHAR2, p_fextend VARCHAR2 DEFAULT '*.fmb');
  
  PROCEDURE prc_exec_from_directory_HOST(p_directory VARCHAR2);
  
END;