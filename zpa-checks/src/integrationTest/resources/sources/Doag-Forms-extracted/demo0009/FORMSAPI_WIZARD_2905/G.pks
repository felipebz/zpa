PACKAGE g IS

  -- F.Matz: 02.02.2017, 18.05.2017   --
  -- Package forr globals.             --
  
  rstate           VARCHAR2(32):= 'INIT';
  result          VARCHAR2(64):= '';
  rcnt            NUMBER(2);
  
  show_state      VARCHAR2(32);
  
  p               VARCHAR2(256)    := 'Init in Package g';
  
  state           VARCHAR2(20)    := 'OK'; -- Global FormsAPI State -- 
  state_last_msg   VARCHAR2(1024)  := '';   -- not used -            --
  
  error_no         VARCHAR2(5);
  error_code       VARCHAR2(10);
  
  C_VLIGHTPOC      CONSTANT VARCHAR2(20):= 'Light-PoC';
  C_VLIGHT        CONSTANT VARCHAR2(20):= 'Light';
  C_VENTERPRISE    CONSTANT VARCHAR2(20):= 'Enterprise';
  
  PROCEDURE msg_push (p_msg VARCHAR2); 
  
  FUNCTION  msg_pop_chk RETURN BOOLEAN; 
  
  FUNCTION  msg_pop  RETURN VARCHAR2;
  
  PROCEDURE msg_free;
  
  
END;
