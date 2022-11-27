PACKAGE BODY pkg_cbox IS
  /*
   * Author:   Friedhold Matz : http://friedhold-matz.blogspot.com/
   * Created:  05.08.2007
   * Modified: 16.09.2007
   * Modified: 29.01.2018
   *
   */
   
  -- Module variables --
  m_CBOX    VARCHAR2(100);
  m_nxtitem VARCHAR2(100);
--------------------------------------------------------------------------------
  PROCEDURE prc_show(p_s VARCHAR2) IS
    al_button PLS_INTEGER;
    al_id     Alert;
  BEGIN
     al_id:= FIND_ALERT('INFO'); 
    SET_ALERT_PROPERTY(al_id, ALERT_MESSAGE_TEXT, p_s ); 
    al_button := SHOW_ALERT( al_id ); 
  END prc_show;
--------------------------------------------------------------------------------
  PROCEDURE populate_auto_cbox (p_list_item varchar2, p_sql_stat varchar2)IS
     rgid recordgroup;
     rcnt number;
     rgname constant varchar2(30):= get_item_property(p_list_item,item_name);
     l_x number(10);
     l_y number(10);
  BEGIN
    -- fix setting : 29.01.2018 - CBOX . --
    m_CBOX   := 'C.CBOX';
    m_nxtitem:= 'C.DUMMY';
    rgid:=find_group(rgname);
    IF id_null(rgid) THEN
        rgid:= create_group_from_query (rgname, p_sql_stat); 
    end if;
    rcnt:=populate_group(rgid);
    IF rcnt=0 THEN 
        populate_list(p_list_item,rgid);
        --copy(get_list_element_value(list_item,1),list_item);
    END IF;
    --delete_group(rgid);
    hide_view('CBX');
    EXCEPTION WHEN others THEN
        prc_show('$$$ Module pkg_Cbox.populate_auto_cbox: '||sqlerrm);
  END populate_auto_cbox;
--------------------------------------------------------------------------------
  PROCEDURE prc_wlc(p_cbox varchar2) IS
     list_id        ITEM;
     list_count    NUMBER;
     current_value  VARCHAR2(250);
     l_found       BOOLEAN := FALSE;
     rgid2         RECORDGROUP;
     gcid2         GROUPCOLUMN; 
  BEGIN
  -- set active CBOX --
  IF pkg_CBOX.m_cbox IS NULL then
     IF p_cbox<>:SYSTEM.cursor_item THEN
        prc_show('$$$ Error in pkg_cbox.prc_wlc: Definition of CBOX.'||sqlerrm);
      END IF;
      pkg_CBOX.m_cbox:= :SYSTEM.cursor_item;  
  END IF;
  --prc_show(pkg_CBOX.g_cbox);  
  IF length(name_in(pkg_CBOX.m_cbox))>1 THEN
     go_item('LABEL');
     -- prc_show(name_in(pkg_CBOX.m_cbox));
  END IF;
  IF name_in(pkg_CBOX.m_cbox) IS NULL THEN
     hide_view('CBX');
     RETURN;
  END IF;
    
  list_id:=FIND_ITEM(pkg_CBOX.m_cbox);
  list_count := GET_LIST_ELEMENT_COUNT(list_id);
 
  go_block('CBX');
  clear_block;
    
  FOR i IN 1..list_count LOOP
    current_value := GET_LIST_ELEMENT_VALUE(list_id, i); 
    IF UPPER(:CBOX)= UPPER(substr(current_value,1,length(name_in(pkg_CBOX.m_cbox)))) then    
       l_found:= TRUE;   
       :CBX.LABEL:= current_value;
       next_record;    
    END IF;   
  END LOOP;
  
  go_item(pkg_CBOX.m_cbox);

  -- fill 
  IF l_found THEN     
     go_item('CBX.LABEL');
     show_view('CBX');
     go_record(1); 
     :CBOX_SAVE:=:CBOX;  
     IF name_in(pkg_CBOX.m_cbox)=:CBX.LABEL THEN
         go_item(pkg_CBOX.m_nxtitem);
        hide_view('CBX');
        copy(Name_IN(pkg_CBOX.m_cbox), pkg_CBOX.m_nxtitem);
     END IF;
  ELSE
     -- 29.01.2018 - F.Matz --
     -- not defined value: wait-finish(<TAB>|<ENTER>) --
     --prc_show('Value not found.');
     RETURN;  
  END IF;  
  
  go_item(pkg_CBOX.m_cbox);
  
  EXCEPTION
  WHEN OTHERS THEN 
      prc_show('$$$ Module pkg_Cbox.prc_wlc: '|| sqlerrm);
  END prc_wlc;
-------------------------------------------------------------------------------- 
  PROCEDURE prc_kni IS
  BEGIN
     IF :CBX.LABEL IS NOT NULL THEN
         copy(:CBX.LABEL, pkg_CBOX.m_cbox);
         copy(Name_IN(pkg_CBOX.m_cbox), pkg_CBOX.m_nxtitem);
      END IF;
     go_item(pkg_CBOX.m_nxtitem);
     hide_view('CBX'); 
      -- copy into next_item --
     copy(Name_IN(pkg_CBOX.m_cbox), pkg_CBOX.m_nxtitem);
  END prc_kni;
--------------------------------------------------------------------------------
  PROCEDURE prc_labelclick IS
  BEGIN
     :CBOX:= :CBX.LABEL;
     go_item(pkg_CBOX.m_CBOX);
     hide_view('CBX');
     -- copy into nex_item --
     copy(Name_IN(pkg_CBOX.m_cbox), pkg_CBOX.m_nxtitem);
  END prc_labelclick;
--------------------------------------------------------------------------------

 BEGIN
     NULL;
    
END;