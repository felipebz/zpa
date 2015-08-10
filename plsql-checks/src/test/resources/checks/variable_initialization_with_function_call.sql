create procedure foo(bar in varchar2 default func(x)) is
  var1 varchar2(1) default func(x); -- violation
  var4 varchar2(1) := func(x); -- violation
  
  type rec is record (field varchar2(1) default func(x));
  
  cursor cur(param in varchar2 default func(x)) is
    select 1 from dual;
begin
  null;
end;