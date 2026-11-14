PACKAGE BODY PKG_SLIDER IS
  
PROCEDURE    Init_Slider
  (
     PC$Name       IN VARCHAR2,
     PN$Num        IN PLS_INTEGER,
     PC$Bounds     IN VARCHAR2
  )
Is

      LC$CVBColor   Varchar2(20) := Get_Canvas_Property( Get_Item_Property( PC$Name, ITEM_CANVAS ), BACKGROUND_COLOR ) ;
      LC$CVFColor   Varchar2(20) := Get_Canvas_Property( Get_Item_Property( PC$Name, ITEM_CANVAS ), FOREGROUND_COLOR ) ;   
      LC$BGColor    Varchar2(20) := Get_Item_Property(PC$Name, BACKGROUND_COLOR) ;
      LC$FGColor    Varchar2(20) := Get_Item_Property(PC$Name, FOREGROUND_COLOR) ;      
      LC$Color      Varchar2(15) ;

Begin 

-- BackGround color --
If LC$BGColor is not null Then
  LC$Color := Translate( LC$BGColor, '0123456789gbr','0123456789,,' ) ;
  Set_Custom_Property( PC$Name, PN$Num, 'SETBGCOLOR', LC$Color ) ;     
Elsif LC$CVBColor is not null Then
  LC$Color := Translate( LC$CVBColor, '0123456789gbr','0123456789,,' ) ;    
  Set_Custom_Property( PC$Name, PN$Num, 'SETBGCOLOR', LC$Color ) ;
Else
      LC$Color := PKG_SLIDER.GC$CurScheme ;
      Set_Custom_Property( PC$Name, PN$Num, 'SETBGCOLOR', LC$Color ) ;
End if ;

-- ForeGround color --
If LC$FGColor is not null Then
  LC$Color := Translate( LC$FGColor, '0123456789gbr','0123456789,,' ) ;
  Set_Custom_Property( PC$Name, PN$Num, 'SETFGCOLOR', LC$Color ) ;
Elsif LC$CVFColor is not null Then
  LC$Color := Translate( LC$CVFColor, '0123456789gbr','0123456789,,' ) ;    
  Set_Custom_Property( PC$Name, PN$Num, 'SETBGCOLOR', LC$Color ) ;  
End if ;
 
-- Bounds --
Set_Custom_Property( PC$Name, PN$Num, 'SETBOUNDS', PC$Bounds ) ;
 
End ;

-------------------------------------------
--  Set the current value of the Slider  --
-------------------------------------------
PROCEDURE Set_Value
  (
     PC$Name       IN VARCHAR2,
     PN$Num        IN PLS_INTEGER,
     PN$Value      IN NUMBER
  )
IS

BEGIN

  -- Initial value --
  Set_Custom_Property( PC$Name, PN$Num, 'SETVALUE', To_Char(PN$Value) ) ;


End Set_Value;

  
END;