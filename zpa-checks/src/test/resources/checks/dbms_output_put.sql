begin
  dbms_output.put('x'); -- Noncompliant {{Avoid direct calls to DBMS_OUTPUT procedures.}}
  dbms_output.put_line('x'); -- Noncompliant
  sys.dbms_output.put('x'); -- Noncompliant
  sys.dbms_output.put_line('x'); -- Noncompliant
  dbms_output.put();
  dbms_output.put_line('x', 'y');
  
  my_output.put_line('x');
  dbms_output.other('x');
  put_line('x');
  x.exists();
end;
