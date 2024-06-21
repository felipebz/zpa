-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/DEPRECATE-pragma.html
BEGIN 
  e := type15_subtype (1 ,1 ,1); 
  d := type15_basetype (1, 1); 
  d.x := 2; -- warning issued 
  d.f1;     -- warning issued 
  e.f1 (4); -- overloaded in derived type. no warning. not deprecated in the derived type. 
  e.f1 (1); -- no warning  
  e.f0;     -- f0 is deprecated in base type. deprecation is inherited. warning issued 
            -- warning issued for deprecated x in d.x and e.x 

  DBMS_OUTPUT.PUT_LINE(to_char(e.x) || to_char(' ') || to_char(d.x)); 

END;