PACKAGE BODY pkg_Item IS

-- Friedhold Matz - 2018-FEB
  
-----------------------------------------------------------------------------------
-- private procs / funcs --
-----------------------------------------------------------------------------------
PROCEDURE prc_enable_item (p_item VARCHAR2) IS
BEGIN
   -- display items only ! --
   Set_Item_Property(p_item , VISIBLE, PROPERTY_TRUE); 
   -- Set_Item_Property(p_item , ENABLED, PROPERTY_TRUE);  
   
END prc_enable_item;
-----------------------------------------------------------------------------------
PROCEDURE sleep (p_x NUMBER) IS
   x NUMBER;
BEGIN
   --FOR i IN 1..p_x LOOP
   --    x:= POWER(2, 100);
   --    SYNCHRONIZE;
   --END LOOP;
   -- FHOFFMANN Changed because the server calculates to slow.
   dbms_lock.sleep(1);
   synchronize;
END sleep;
-----------------------------------------------------------------------------------
FUNCTION fnc_get_txtnnullc (p_bit VARCHAR2, p_txt VARCHAR2)RETURN VARCHAR2 IS
BEGIN
   IF pkg_Item.item_name(p_bit).notnull='YES' THEN
       RETURN(p_txt||' *');
   ELSE
       RETURN(p_txt);
   END IF;
END fnc_get_txtnnullc;
-----------------------------------------------------------------------------------
FUNCTION fnc_sign_msg (p_txt VARCHAR2) RETURN VARCHAR2 IS
BEGIN
   CASE p_txt 
       WHEN 'LIGHT'  THEN RETURN('VA_TXT_LIGHT_MSG'); 
       WHEN 'MEDIUM' THEN RETURN('VA_TXT_MEDIUM_MSG'); 
       WHEN 'STRONG' THEN RETURN('VA_TXT_STRONG_MSG'); 
   ELSE
      RETURN('VA_TXT_ERROR_MSG');
   END CASE; 

END fnc_sign_msg;
-----------------------------------------------------------------------------------

-----------------------------------------------------------------------------------
-- global procs / funcs --
-----------------------------------------------------------------------------------
PROCEDURE prc_Enter IS
   l_blk    VARCHAR2(32);
   l_fit     VARCHAR2(32);
   l_bit     VARCHAR2(32);
BEGIN      
   l_fit:= :SYSTEM.CURSOR_ITEM;  
   -- get blank item name --
   l_bit:= substr(l_fit, instr(l_fit,'.')+1, length(l_fit)-instr(l_fit,'.'));  
   l_blk:= pkg_Item.item_name(l_bit).block;
   -- clear item --
   COPY(NULL, l_fit); 
   -- clear msg  --
   COPY(NULL, l_blk||'.'||'MSG_'||l_bit);
   -- activate underline      --
   Set_Item_Property(l_blk||'.'||'UNDER_'||l_bit, VISUAL_ATTRIBUTE, 'VA_UL_ACTIVE');  
   -- activate label          --
   prc_enable_item(l_blk||'.'||'LABEL_'||l_bit);
   Set_Item_Property(l_blk||'.'||'LABEL_'||l_bit, VISUAL_ATTRIBUTE, 'VA_TXT_LABEL_ACTIVE');   
   -- deactivate msg --
   Set_Item_Property(l_blk||'.'||'MSG_'||l_bit , VISIBLE, PROPERTY_FALSE);  
   -- activate input property --  
   Set_Item_Property(l_fit , VISUAL_ATTRIBUTE, 'VA_TEXT');   
   --
   IF pkg_Item.item_name(l_bit).type='SECURE' THEN
      -- hide text --
      Set_item_Property(l_blk||'.'||l_bit, ECHO, PROPERTY_FALSE);        
   END IF; 
   
  EXCEPTION WHEN OTHERS THEN
     prc_info('$$$ EXCEPTION in pkg_Item.prc_Enter: '||sqlerrm);    
END prc_Enter;
-----------------------------------------------------------------------------------
PROCEDURE prc_Leave IS
  l_blk      VARCHAR2(32);
  l_fit      VARCHAR2(32);
  l_bit      VARCHAR2(32);
  l_it_value VARCHAR2(64);
  l_vres     VARCHAR2(512);  
BEGIN    
  l_fit      := :SYSTEM.CURSOR_ITEM; 
  l_it_value := NAME_IN(l_fit); 
  l_bit      := substr(l_fit, instr(l_fit,'.')+1, length(l_fit)-instr(l_fit,'.'));  
  l_blk      := pkg_Item.item_name(l_bit).block;
  
  IF l_it_value IS NULL OR l_it_value=pkg_Item.item_name(l_bit).text THEN      
     COPY(pkg_Item.item_name(l_bit).text, l_fit);
     COPY(pkg_Item.item_name(l_bit).label, l_blk||'.'||'LABEL_'||l_fit);  
     -- underline to empty  --
      Set_Item_Property(l_blk||'.'||'UNDER_'||l_bit , VISUAL_ATTRIBUTE, 'VA_UL_EMPTY');        
     -- deactivate label    --
     Set_Item_Property(l_blk||'.'||'LABEL_'||l_bit , VISIBLE, PROPERTY_FALSE);  
     --
     IF pkg_Item.item_name(l_bit).type='SECURE' THEN
        -- hide text --
        Set_item_Property(l_blk||'.'||l_bit, ECHO, PROPERTY_TRUE);        
     END IF;    
     -- activate insert property --  
     Set_Item_Property(l_fit , VISUAL_ATTRIBUTE, 'VA_TEXT_INSERT');     
     
   ELSE
     l_vres:= fnc_validate(l_bit);
     IF substr(l_vres,1,3)<>'$$$' THEN
         IF l_vres NOT IN ('OK', 'LIGHT', 'MEDIUM', 'STRONG') THEN
            Set_Item_Property(l_blk||'.'||'LABEL_'||l_bit , VISUAL_ATTRIBUTE, 'VA_TXT_LABEL_ERROR');  
            -- underline to error full  --
            prc_enable_item(l_blk||'.'||'UNDER_'||l_bit);
             Set_Item_Property(l_blk||'.'||'UNDER_'||l_bit, VISUAL_ATTRIBUTE, 'VA_UL_ERROR'); 
            COPY(l_vres, l_blk||'.'||'MSG_'||l_bit);
            -- Set_Item_Property(pkg_Item.item_name(l_bit).block||'.'||'MSG_'||l_bit , VISUAL_ATTRIBUTE, fnc_sign_msg(l_vres));
            prc_enable_item(l_blk||'.'||'MSG_'||l_bit);
         ELSE
            Set_Item_Property(l_blk||'.'||'LABEL_'||l_bit , VISUAL_ATTRIBUTE, 'VA_TXT_LABEL_OK');
            prc_enable_item(l_blk||'.'||'UNDER_'||l_bit);
             Set_Item_Property(l_blk||'.'||'UNDER_'||l_bit , VISUAL_ATTRIBUTE, 'VA_UL_FULL'); 
             IF l_vres<>'OK' THEN
                prc_enable_item(l_blk||'.'||'MSG_'||l_bit);
                Set_Item_Property(l_blk||'.'||'MSG_'||l_bit , VISUAL_ATTRIBUTE, fnc_sign_msg(l_vres));
                COPY(l_vres, l_blk||'.'||'MSG_'||l_bit);
             END IF;
         END IF;
      ELSE
         prc_enable_item(l_blk||'.'||'LABEL_'||l_bit);   
         Set_Item_Property(l_blk||'.'||'LABEL_'||l_bit , VISUAL_ATTRIBUTE, 'VA_TXT_LABEL_ERROR');
        -- underline to error full  --
         prc_enable_item(l_blk||'.'||'UNDER_'||l_bit);
         Set_Item_Property(l_blk||'.'||'UNDER_'||l_bit, VISUAL_ATTRIBUTE, 'VA_UL_ERROR'); 
         -- error msg -- 
        COPY(l_vres, l_blk||'.'||'MSG_'||l_bit);
        prc_enable_item(l_blk||'.'||'MSG_'||l_bit);  
        Set_Item_Property(l_blk||'.'||'MSG_'||l_bit , VISUAL_ATTRIBUTE, 'VA_TXT_ERROR_MSG');         
     END IF;    
  END IF; 
  
  EXCEPTION WHEN OTHERS THEN
     prc_info('$$$ EXCEPTION in pkg_Item.prc_Leave: '||sqlerrm);   
END prc_Leave;
-----------------------------------------------------------------------------------
FUNCTION fnc_final_check RETURN VARCHAR2 IS
   l_value VARCHAR2(64);
BEGIN
   FOR i IN 1.. pkg_Item.item_ix.count LOOP
        l_value:= NAME_IN(pkg_Item.item_ix(i).name);
       IF (l_value IS NULL AND pkg_Item.item_ix(i).notnull='YES') OR 
           -- item label text ? --
           (l_value=pkg_Item.item_ix(i).text AND pkg_Item.item_ix(i).notnull='YES') OR
           -- item error message ? --
           substr(NAME_IN(pkg_Item.item_name(pkg_Item.item_ix(i).name).block||'.'||
                          'MSG_'||pkg_Item.item_ix(i).name),1,3)='$$$' THEN          
           RETURN('Item: '|| pkg_Item.item_ix(i).name);
       END IF;      
   END LOOP;
   
   RETURN('OK'); 

 EXCEPTION WHEN OTHERS THEN
     prc_info('$$$ EXCEPTION in pkg_Item.fnc_final_check: '||sqlerrm);      
END fnc_final_check;
-----------------------------------------------------------------------------------
PROCEDURE prc_chk_item (p_block VARCHAR2, p_item VARCHAR2, p_value VARCHAR2, p_result VARCHAR2 DEFAULT NULL ) IS
   l_res VARCHAR2(16);  
BEGIN  
   go_item(p_block||'.'||p_item);
   IF  p_value='GO' THEN 
       RETURN;
   ELSIF p_value='PRESS' THEN
        Execute_Trigger('WHEN-BUTTON-PRESSED');  
        sleep(100);
   ELSE 
        -- setter & getter item values --
       Execute_Trigger('WHEN-NEW-ITEM-INSTANCE');  
       Copy(p_value, p_block||'.'||p_item);
       Execute_Trigger('WHEN-VALIDATE-ITEM');     
       sleep(150);      
          
       -- check expected/real result -- 
       l_res:='OK';
       IF (substr(NAME_IN(pkg_Item.item_name(p_item).block||'.'||'MSG_'||p_item) ,1,3)='$$$') THEN
           l_res:='NOK';
       END IF;
       IF (l_res='NOK' AND p_result='OK') OR
          ( (Name_In('MSG_'||p_item) IS NULL OR l_res='OK') AND p_result='NOK' )
          THEN
            prc_info('$$$ Error in automatic test sequence :: '||chr(10)||
                     'Item: '||p_item||chr(10)||
                     'Value:'||p_value||chr(10)||
                     'Result expected: '||p_result||chr(10)||
                     'Result real: '||l_res||chr(10)||' $$$');
          RETURN;
       END IF;       
       -- compare values : setter=getter ? --
       IF Name_In(pkg_Item.item_name(p_item).block||'.'||p_item)<>p_value THEN
           prc_info('$$$ Error in automatic test sequence :: '||chr(10)||
                    'Item: '||p_item||chr(10)||
                    'Value expected: '||p_value||chr(10)||
                    'Value real: '||Name_In(pkg_Item.item_name(p_item).block||'.'||p_item)||chr(10)||' $$$');
       END IF;   
   END IF; 

  EXCEPTION WHEN OTHERS THEN
     prc_info('$$$ Exception in pkg_Item.prc_chk_Item: '||sqlerrm); 
END prc_chk_item;
-----------------------------------------------------------------------------------
-- !!! Used prc_Set_Items - defined from USER external procedure !!! --
-----------------------------------------------------------------------------------
PROCEDURE prc_rec (p_ix PLS_INTEGER, p_block VARCHAR2, p_name VARCHAR2, p_label VARCHAR2, p_text VARCHAR2, 
                   p_messg VARCHAR2 DEFAULT NULL, p_notnull VARCHAR2 DEFAULT 'YES', p_type VARCHAR2 DEFAULT 'NORMAL') IS
    l_label VARCHAR2(64);
    l_text  VARCHAR2(64);
BEGIN    
   IF p_notnull='YES' THEN
      l_label:= p_label||' *';
      l_text := p_text||' *';
   ELSE
      l_label:= p_label;
      l_text := p_text;
   END IF;
   -- 1. set name sorted tab --
   pkg_Item.item_name(p_name).id      := p_ix;
   pkg_Item.item_name(p_name).block   := p_block;
   pkg_Item.item_name(p_name).name    := p_name;
   pkg_Item.item_name(p_name).label   := l_label;
   pkg_Item.item_name(p_name).text    := l_text;
   pkg_Item.item_name(p_name).msg     := p_messg;  
   pkg_Item.item_name(p_name).notnull := p_notnull;   
   pkg_Item.item_name(p_name).type     := p_type;
            
   -- 2. set ix sorted tab --
    pkg_Item.item_ix(p_ix).id          := p_ix;
   pkg_Item.item_ix(p_ix).block       := p_block;
   pkg_Item.item_ix(p_ix).name        := p_name;
    pkg_Item.item_ix(p_ix).label       := l_label;
   pkg_Item.item_ix(p_ix).text        := l_text;
   pkg_Item.item_ix(p_ix).msg         := p_messg;
   pkg_Item.item_ix(p_ix).notnull     := p_notnull;
   pkg_Item.item_ix(p_ix).type        := p_type;  
                 
  EXCEPTION WHEN OTHERS THEN
     prc_info('$$$ Exception in pkg_Item.prc_init(rec): '||sqlerrm); 
END prc_rec;

PROCEDURE prc_init_Items IS 
  l_name VARCHAR2(32);
 BEGIN   
    -- USER DEFINED - external !!! --
    prc_Set_Items;
    
   FOR i IN 1.. pkg_Item.item_ix.count LOOP
       l_name:= pkg_Item.item_ix(i).name;          
       COPY(pkg_Item.item_ix(i).text,  l_name);  
       COPY(pkg_Item.item_ix(i).label, 'LABEL_'||l_name);   
       -- specials --
       IF pkg_Item.item_ix(i).type='SECURE' THEN
          -- shows text --
          Set_item_Property(pkg_Item.item_ix(i).block||'.'||l_name, ECHO, PROPERTY_TRUE);        
       END IF;     
       Set_item_Property(pkg_Item.item_ix(i).block||'.UNDER_'||l_name, VISUAL_ATTRIBUTE, 'VA_UL_EMPTY');                
   END LOOP;
  
 EXCEPTION WHEN OTHERS THEN
    prc_info('$$$ EXCEPTION pkg_Item.prc_init: '||sqlerrm);  
 END prc_init_Items;

-----------------------------------------------------------------------------------

END pkg_Item;