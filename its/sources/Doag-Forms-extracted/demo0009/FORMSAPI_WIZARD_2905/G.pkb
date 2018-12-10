PACKAGE BODY g IS
   -- F.Matz: 02.02.107 
    
   TYPE t_var_msg IS VARRAY(100) OF VARCHAR2(256); 
   
   var_msg t_var_msg := t_var_msg('none','none','none','none','none','none','none','none','none','none',
                                   'none','none','none','none','none','none','none','none','none','none',
                                   'none','none','none','none','none','none','none','none','none','none',
                                   'none','none','none','none','none','none','none','none','none','none',
                                   'none','none','none','none','none','none','none','none','none','none',
                                   'none','none','none','none','none','none','none','none','none','none',
                                   'none','none','none','none','none','none','none','none','none','none',
                                   'none','none','none','none','none','none','none','none','none','none',
                                   'none','none','none','none','none','none','none','none','none','none',
                                   'none','none','none','none','none','none','none','none','none','none');
                                   
     C_MAX_VAR CONSTANT NUMBER(4):=100;
     m_ix   PLS_INTEGER:=1; 
     m_cntw PLS_INTEGER:=0;
     m_cntr PLS_INTEGER:=0;    
     
   PROCEDURE msg_push(p_msg VARCHAR2) IS
   BEGIN
       var_msg(m_ix):= p_msg;  
       IF m_ix=C_MAX_VAR THEN
          m_ix:=0;             -- loop
       END IF;
       m_ix:= m_ix+1;           -- ix at next free !
       var_msg(m_ix):= 'none';
       m_cntw:= m_cntw+1;
   END msg_push;
   
   FUNCTION  msg_pop_chk RETURN BOOLEAN IS
      l_ix NUMBER(4);
      l_msg VARCHAR2(256);
   BEGIN
       l_ix:= m_ix;
      IF l_ix=1 THEN
          l_msg:= var_msg(C_MAX_VAR);
       ELSE
          l_msg:= var_msg(l_ix-1);
       END IF;
       RETURN(l_msg<>'none');
   END msg_pop_chk;
       
   FUNCTION  msg_pop RETURN VARCHAR2 IS
      l_msg VARCHAR(256);
   BEGIN 
       IF m_ix=1 THEN
          l_msg:= var_msg(C_MAX_VAR);
          var_msg(C_MAX_VAR):= 'none';
          m_ix:= C_MAX_VAR;
       ELSE
          m_ix:= m_ix-1;
          l_msg:= var_msg(m_ix);
          var_msg(m_ix):= 'none';
       END IF;
       RETURN(l_msg);
   END msg_pop;
   
   PROCEDURE msg_free IS
      l_msg VARCHAR(256);
   BEGIN 
       IF m_ix=1 THEN
          l_msg:= var_msg(C_MAX_VAR);
          var_msg(C_MAX_VAR):= 'none';
          m_ix:= C_MAX_VAR;
       ELSE
          m_ix:= m_ix-1;
          l_msg:= var_msg(m_ix);
          var_msg(m_ix):= 'none';
       END IF;
       RETURN;            -- dummy pop --
   END msg_free;
      
END;