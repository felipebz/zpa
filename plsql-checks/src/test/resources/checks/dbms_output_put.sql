begin
  dbms_output.put('x'); -- violation
  dbms_output.put_line('x'); -- violation
  sys.dbms_output.put_line('x'); -- violation
  
  my_output.put_line('x');
end;