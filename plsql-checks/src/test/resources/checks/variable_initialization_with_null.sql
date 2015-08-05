create procedure foo(bar in varchar2 default null) is -- parameter declaration, no issue
  var1 varchar2(1) default null; -- violation
  var2 varchar2(1) default ''; -- violation
  var3 varchar2(1) := null; -- violation
  var4 varchar2(1) := ''; -- violation
  
  type rec is record (field varchar2(1) default null); -- violation
  
  cursor cur(param in varchar2 default null) is -- cursor parameter, no issue
    select 1 from dual;
begin
  null;
end;