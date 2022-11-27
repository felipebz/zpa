PACKAGE pkg_Item IS

  -- Friedhold Matz - 2018-FEB
  
  gLastItem VARCHAR2(128);
  
  C_BlockName CONSTANT VARCHAR2(32):= 'BLK_ACCOUNT';
     
  TYPE rec_def_t IS RECORD (id       NUMBER(4),    
                            block    VARCHAR2(32),
                            name     VARCHAR2(32), 
                            label    VARCHAR2(64), 
                            text      VARCHAR2(64), 
                            msg      VARCHAR2(64),
                            notnull VARCHAR2(8),
                            type     VARCHAR2(32)
                           );
                           

  TYPE rec_item_name_t IS TABLE OF rec_def_t INDEX BY VARCHAR2(32);   -- order by code
  TYPE rec_item_ix_t   IS TABLE OF rec_def_t INDEX BY PLS_INTEGER;   -- order by index

  item_name   rec_item_name_t;  
  item_ix     rec_item_name_t;

-- WHEN-NEW-INSTANCE-ITEM trigger --
PROCEDURE prc_Enter;

-- WHEN-VALIDATE-ITEM trigger --
PROCEDURE prc_Leave;

-- e.g. KEX-EXIT trigger --
FUNCTION fnc_final_check RETURN VARCHAR2;

-- automated checks a item --
PROCEDURE prc_chk_item (p_block VARCHAR2, p_item VARCHAR2, p_value VARCHAR2, p_result VARCHAR2 DEFAULT NULL);

PROCEDURE prc_rec (p_ix PLS_INTEGER, p_block VARCHAR2, p_name VARCHAR2, p_label VARCHAR2, p_text VARCHAR2, 
                 p_messg VARCHAR2 DEFAULT NULL, p_notnull VARCHAR2 DEFAULT 'YES', p_type VARCHAR2 DEFAULT 'NORMAL');

PROCEDURE prc_init_Items;
                
END pkg_Item;