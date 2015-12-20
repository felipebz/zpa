create procedure foo(a number, b number) is -- violation on "b"
begin
  print(a);
end;
/

create function bar(a number, b number) return number is  -- violation on "b"
begin
  return a;
end;
/

create package test is
  procedure foo(a number, b number); -- don't report violation on headings
  
   procedure foo(a number, b number) is -- violation on "b"
     cursor cur(x number) is -- violation on "x"
       select 1 from dual;
   begin
     print(a);
   end;
end;
/
