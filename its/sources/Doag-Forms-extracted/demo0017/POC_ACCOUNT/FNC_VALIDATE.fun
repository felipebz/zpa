FUNCTION fnc_validate (p_item VARCHAR2) RETURN VARCHAR2 IS

  -- Friedhold Matz - 2018-FEB
  
  l_vres VARCHAR2(256);
BEGIN  
  IF p_item='EMAIL' THEN
     IF regexp_like(:BLK_ACCOUNT.EMAIL, 
        '([a-zA-Z0-9_\-\.]+)@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.)|(([a-zA-Z0-9\-]+\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})')
        THEN
         RETURN('OK');
     ELSE
          RETURN('$$$ Error: '|| pkg_Item.item_name('EMAIL').msg ||' $$$');
     END IF;
  ELSIF p_item='EMAIL2' THEN
     IF LOWER(:BLK_ACCOUNT.EMAIL)=LOWER(:BLK_ACCOUNT.EMAIL2) THEN
         RETURN('$$$ Error: eMail2 is the same as eMail ! $$$');
     END IF;
     IF regexp_like(:BLK_ACCOUNT.EMAIL2, 
        '([a-zA-Z0-9_\-\.]+)@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.)|(([a-zA-Z0-9\-]+\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})')
       THEN
         RETURN('OK');
     ELSE
          RETURN('$$$ Error: '|| pkg_Item.item_name('EMAIL2').msg ||' $$$');
     END IF;
  ELSIF p_item='PASSWORD' THEN
     IF :BLK_ACCOUNT.PASSWORD<>pkg_Item.item_name('PASSWORD').text THEN
        l_vres:=v#r#fy_pw$001(:BLK_ACCOUNT.USERNAME,:BLK_ACCOUNT.PASSWORD);
        IF l_vres='OK' THEN
            RETURN('OK');
        ELSE
           RETURN(l_vres);
        END IF;
     ELSE
        RETURN('$$$ Error: Username and Password are not completed ! $$$');
     END IF;
  ELSIF p_item='PASSWORD_RETRY' THEN
     IF :PASSWORD<>:PASSWORD_RETRY THEN
         RETURN('$$$ Error: Passwords are not identical ! $$$');
     END IF;
  END IF;
  
  RETURN('OK');
  
EXCEPTION WHEN OTHERS THEN
  RETURN('$$$ EXCEPTION in fnc_validate) - item: '||p_item||' : '||sqlerrm);
END;