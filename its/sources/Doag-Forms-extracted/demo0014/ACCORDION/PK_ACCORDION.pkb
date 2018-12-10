PACKAGE BODY PK_ACCORDION IS

  TYPE tList IS TABLE OF tAccordionList INDEX BY VARCHAR2(30);
  lList tList;

  -- --------------------------------------------------------------------------------------
 
  PROCEDURE PR_INIT_ACCORDION(i_vcAccordionGroup IN VARCHAR2, i_lAccordionList IN tAccordionList) IS
    vcPrior VARCHAR2(4000);
    vcNext  VARCHAR2(4000);
  BEGIN
    lList(i_vcAccordionGroup):=i_lAccordionList;
    FOR i IN 1..i_lAccordionList.COUNT LOOP
      -- prior Accordion-entry
      IF i>1 THEN
        vcPrior:=i_lAccordionList(i-1).vcButton;
      ELSE
        vcPrior:='.';
      END IF;
      -- following Accordion-entries
      IF i<i_lAccordionList.COUNT THEN
        vcNext:=i_lAccordionList(i+1).vcButton;
      ELSE
        vcNext:='.';
      END IF;
      SET_CUSTOM_ITEM_PROPERTY(i_lAccordionList(i).vcButton, 'INIT_ACCORDION', i_lAccordionList(i).vcButton || '|' ||
                                                                               CASE WHEN i_lAccordionList(i).bOpened THEN 
                                                                                 'J'
                                                                               ELSE
                                                                                 'N'
                                                                               END     || '|' ||  
                                                                               vcPrior || '|' ||  
                                                                               vcNext  || '|' ||
                                                                               NVL(i_lAccordionList(i).vcExpandedImage, '.')  || '|' ||
                                                                               NVL(i_lAccordionList(i).vcCollapsedImage, '.') || '|'
                              );
    END LOOP;
    SYNCHRONIZE;
    -- Make first item scale, it will delegate to others
    SET_CUSTOM_ITEM_PROPERTY(i_lAccordionList(1).vcButton, 'SCALE_ACCORDION', ' ');
  END;
 
  -- --------------------------------------------------------------------------------------
 
  PROCEDURE PR_ACTIVATE(i_vcAccordionGroup IN VARCHAR2, i_vcCanvas IN VARCHAR2) IS
    lAccordionList tAccordionList;
  BEGIN
    IF lList.EXISTS(i_vcAccordionGroup) THEN
      lAccordionList:=lList(i_vcAccordionGroup);
      FOR i IN 1..lAccordionList.COUNT LOOP
        IF lAccordionList(i).vcCanvas=i_vcCanvas THEN
          SET_CUSTOM_ITEM_PROPERTY(lAccordionList(i).vcButton, 'ACTIVATE', ' ');
          EXIT;
        END IF;
      END LOOP;
    END IF;
  END;
    
  -- --------------------------------------------------------------------------------------
 
  PROCEDURE PR_GO_ITEM(i_vcItem IN VARCHAR2) IS
    lAccordionList tAccordionList;
    vcCanvas       VARCHAR2(100);
    vcGroup        VARCHAR2(30);
    itId           ITEM;
  BEGIN
    -- find the item canvas --
    itId:=FIND_ITEM(i_vcItem);
    IF NOT ID_NULL(itId) THEN
      vcCanvas:=GET_ITEM_PROPERTY(itId, ITEM_CANVAS ) ;
      -- find the accordion group --
      vcGroup:=lList.FIRST ;
      LOOP
        EXIT WHEN vcGroup IS NULL;
        lAccordionList:=lList(vcGroup); 
        FOR i IN 1..lAccordionList.COUNT LOOP
          IF UPPER(lAccordionList(i).vcCanvas) = vcCanvas THEN
            PR_ACTIVATE(vcGroup, vcCanvas);
            GO_ITEM(i_vcItem);
            RETURN;
          END IF;
        END LOOP;
        vcGroup:=lList.NEXT(vcGroup) ;
      END LOOP;
    END IF;
  END;
   
  -- --------------------------------------------------------------------------------------
 
  PROCEDURE PR_GO_BLOCK(i_vcBlock IN VARCHAR2) IS
    lAccordionList tAccordionList;
    vcCanvas       VARCHAR2(100);
    vcGroup        VARCHAR2(30);
    vcItem         VARCHAR2(61);
    blId           BLOCK;
    bOk            BOOLEAN:= FALSE ;
  BEGIN
    -- find the canvas --
    blId := FIND_BLOCK(i_vcBlock);
    IF NOT ID_NULL(blId) THEN
      vcItem:= GET_BLOCK_PROPERTY(blId, FIRST_ITEM);
      LOOP
        vcCanvas:= GET_ITEM_PROPERTY(vcItem, ITEM_CANVAS ) ;
        IF vcCanvas IS NOT NULL THEN
          bOk:=TRUE;
          EXIT;
        END IF ; 
        vcItem:= GET_ITEM_PROPERTY(vcItem, NEXTITEM ) ;
        EXIT WHEN vcItem IS NULL;
      END LOOP;
      IF bOk THEN
        -- find the accordion group --
        vcGroup:=lList.FIRST;
        LOOP
          EXIT WHEN vcGroup IS NULL;
          lAccordionList:=lList(vcGroup); 
          FOR i IN 1..lAccordionList.COUNT LOOP
            If UPPER(lAccordionList(i).vcCanvas)=vcCanvas THEN
              PR_ACTIVATE(vcGroup, vcCanvas);
              GO_BLOCK(i_vcBlock);
              RETURN;
            END IF;
          END LOOP;
          vcGroup:=lList.NEXT(vcGroup);
        END LOOP; 
      END IF;
    END IF ;
  END PR_GO_BLOCK;  
END;