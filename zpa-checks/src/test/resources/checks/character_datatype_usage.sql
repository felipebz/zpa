declare
  var1 varchar(10); -- Noncompliant {{Use VARCHAR2 instead of VARCHAR.}}
  var2 char(10); -- Noncompliant {{Use VARCHAR2 instead of CHAR.}}
  
  var3 varchar2(10); -- compliant
begin
  null;
end;