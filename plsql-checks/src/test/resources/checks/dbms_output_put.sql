begin
  dbms_output.put('x'); -- Noncompliant {{Avoid direct calls to DBMS_OUTPUT procedures.}}
  dbms_output.put_line('x'); -- Noncompliant
  sys.dbms_output.put_line('x'); -- Noncompliant
  
  my_output.put_line('x');
  dbms_output.other('x');
  put_line('x');
  x.exists();
end;