FUNCTION fnc_msg_query(p_msg VARCHAR2) RETURN VARCHAR2 IS
   l_button PLS_INTEGER;
   l_id     Alert;
   l_res VARCHAR2(32);
BEGIN
   l_id:= FIND_ALERT('QUERY'); 
    SET_ALERT_PROPERTY(l_id, ALERT_MESSAGE_TEXT, p_msg ); 
   l_button := SHOW_ALERT( l_id ); 
   IF l_button = ALERT_BUTTON1 THEN
         l_res := 'YES';
      ELSIF l_button = ALERT_BUTTON2 THEN
         l_res := 'NO';
      ELSE
         l_res := 'CANCEL';
   END IF;
      
   RETURN(l_res);
  
END fnc_msg_query;