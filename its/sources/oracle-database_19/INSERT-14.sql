INSERT INTO lob_tab 
   SELECT pic_id, TO_LOB(long_pics) FROM long_tab;