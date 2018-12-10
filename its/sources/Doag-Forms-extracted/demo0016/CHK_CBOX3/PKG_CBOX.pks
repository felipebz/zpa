PACKAGE pkg_cbox IS
  
  PROCEDURE prc_show(p_s varchar2);
  PROCEDURE populate_auto_cbox (p_list_item varchar2, p_sql_stat varchar2);
  PROCEDURE prc_wlc(p_cbox varchar2);
  PROCEDURE prc_kni;
  PROCEDURE prc_labelclick;
  
END;