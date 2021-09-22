create procedure foo(bar in varchar2 default null) is -- parameter declaration, no issue
  var1 varchar2(1) default null; -- Noncompliant {{Remove this unnecessary initialization to NULL.}}
  var2 varchar2(1) default ''; -- Noncompliant
  var3 varchar2(1) := null; -- Noncompliant
  var4 varchar2(1) := ''; -- Noncompliant
  
  type rec is record (field varchar2(1) default null); -- Noncompliant
  
  cursor cur(param in varchar2 default null) is -- cursor parameter, no issue
    select 1 from dual;
begin
  null;
end;