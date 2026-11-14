PACKAGE PKG_SLIDER IS
  
 -- Colorscheme RGB values --
  GC$Teal      Varchar2(15) := '115,142,140' ;
  GC$Titanium  Varchar2(15) := '99,101,99' ;
  GC$Red       Varchar2(15) := '156,130,123' ;  
  GC$Khaki     Varchar2(15) := '140,142,123' ;        
  GC$Blue      Varchar2(15) := '90,117,148' ;
  GC$Olive     Varchar2(15) := '107,113,99' ;  
  GC$Purple    Varchar2(15) := '123,113,140' ;        
  GC$Blaf      Varchar2(15) := '247,247,231' ;

  -- Current colorscheme --
  GC$CurScheme Varchar2(15) := '' ;


  PROCEDURE    Init_Slider
  (
     PC$Name       IN VARCHAR2,
     PN$Num        IN PLS_INTEGER,
     PC$Bounds     IN VARCHAR2
  ) ;

  PROCEDURE Set_Value
  (
     PC$Name       IN VARCHAR2,
     PN$Num        IN PLS_INTEGER,
     PN$Value      IN NUMBER
  ) ;
 
  
END;