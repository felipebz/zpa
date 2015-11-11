begin
  -- this is a comment
  /*
   * another comment
   */
  for i in 1..2 loop
  
    dbms_output.put_line(i);
  
  end loop;
  
  begin
    null;
  end;
exception
  when no_data_found then
    null;
  when too_many_rows then
    null; 
end;
/
create procedure test is begin null; end;
/
create function foo return number is begin null; end;
/