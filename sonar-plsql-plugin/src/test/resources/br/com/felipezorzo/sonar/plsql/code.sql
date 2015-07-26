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
end;
/