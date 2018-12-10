FUNCTION v#r#fy_pw$001 (    
    p_username      varchar2,
    p_password      varchar2
) RETURN VARCHAR2 IS

 /* 
  * This password check enabled some special characters using with "my :-} password.§$"
  * and get the password strength in Oracle Forms for the Oracle DB password setting.
  * That's a maximal password variant; remember that you can use ANY characters
  * in Oracle DB enclosed in double quotes e.g. " . - # ~ 12 .."
  * OUTPUT: substr(v#r#fy_pw$001,1,3)<>'$$$' => {LIGHT|MEDIUM|STRONG} :: 'OK'
  *         substr(v#r#fy_pw$001,1,3)= '$$$' => '$$$ Error .. $$$' .
  *
  * Friedhold Matz - 2018-FEB
  *
 **/
   -- password strength definition --
   C_MINPWLEN    CONSTANT NUMBER(3) :=12;
   C_MINDIGIT    CONSTANT NUMBER(3) :=3;
   C_MINCHARLOW  CONSTANT NUMBER(3) :=3;
   C_MINCHARUPP  CONSTANT NUMBER(3) :=3;
   C_MINSPECIAL  CONSTANT NUMBER(3) :=3;
   C_MEDIUM      CONSTANT NUMBER(2) :=17;
   C_STRONG      CONSTANT NUMBER(2) :=20;
   
   l_lenpw        NUMBER(2);
   l_restype      VARCHAR2(32);
   l_cnt_charlow  NUMBER(3) :=0;
   l_cnt_charupp  NUMBER(3) :=0;
   l_cnt_digit    NUMBER(3) :=0;
   l_cnt_special  NUMBER(3) :=0;
   l_cnt_NO       NUMBER(3) :=0;
   l_1un          CHAR(1);
   l_lenun        NUMBER(2);
   l_char         CHAR(1);

BEGIN 
   -- Check for the minimum length of the password --
   l_lenpw := length(p_password);
   IF l_lenpw < C_MINPWLEN THEN
      RETURN('$$$ Error: Password length less than '||C_MINPWLEN||' characters. $$$');
   END IF;
   -- Check if the password is same as the username or username(1-100)
   IF LOWER(password) = LOWER(p_username) THEN
     RETURN('$$$ Error: Password same as or similar to user $$$');
   END IF;
   l_lenun := length(p_username);
   l_1un   := substr(p_username,1,1);
   ------------------------------------------------------------------------------
   --- Friedhold Matz : 14.10.2013 / 14.12.2017 / 09.02.2018                       ---
   ------------------------------------------------------------------------------
   FOR i IN 1..l_lenpw LOOP 
       l_char := substr(p_password, i ,1);      
       IF l_char BETWEEN 'a' AND 'z' THEN    
          l_cnt_charlow:= l_cnt_charlow+1;
       ELSIF l_char BETWEEN 'A' and 'Z' THEN
          l_cnt_charupp:= l_cnt_charupp+1;         
       ELSIF l_char BETWEEN '0' AND '9' THEN
          l_cnt_digit:= l_cnt_digit+1;    
       ELSIF l_char IN( '#', '_', '$', '!', '"', '§', '%', '&', '/', '(', ')', '=', '?', '\', '{', '>', '<', '`',  '°',
                         '[', ']', '}', '~', '+', '*', '#', '-', ';', ',', ':', '.', ':', ' ', '´', ' ', '|', '''', '^' ) THEN
          l_cnt_special:= l_cnt_special+1;         
       ELSE
          l_cnt_NO := l_cnt_NO+1;
       END IF;
       IF LOWER(l_char)=LOWER(l_1un) THEN
           IF LOWER(p_username)=LOWER(substr(p_password,i,l_lenun)) THEN
              RETURN('$$$ Error: Username is included in Password ! $$$');
           END IF;
       END IF;
   END LOOP;   
   IF l_cnt_charlow<C_MINCHARLOW THEN
      RETURN('$$$ Error: Password does not incl. min. '||C_MINCHARLOW||' lower case characters. $$$ ');
   END IF;
   IF l_cnt_charupp<C_MINCHARUPP THEN
      RETURN('$$$ Error: Password does not incl. min. '||C_MINCHARUPP||' upper case characters. $$$ ');
   END IF;
   IF l_cnt_digit<C_MINDIGIT THEN
      RETURN('$$$ Error: Password does not incl. min. '||C_MINDIGIT||' digit characters. $$$ ');
   END IF;
   IF l_cnt_special<C_MINSPECIAL THEN
      RETURN('$$$ Error: Password does not incl. min. '||C_MINSPECIAL||' special characters. $$$ ');
   END IF;   
   IF l_cnt_NO>0 THEN
      RETURN('$$$ Error: Password contains invalid characters. $$$');
   END IF;
   ------------------------------------------------------------------------------  
   --- Everything is fine, get the strength now. ---  
   l_restype:='LIGHT';
   IF l_lenpw BETWEEN C_MEDIUM AND C_STRONG THEN
       l_restype:='MEDIUM';
   ELSIF l_lenpw > C_STRONG THEN
       l_restype:='STRONG';
   END IF;
   
   RETURN (l_restype);

EXCEPTION WHEN OTHERS THEN
   RETURN ('$$$ : '||l_lenpw||' / '||sqlerrm);  
END  v#r#fy_pw$001;
