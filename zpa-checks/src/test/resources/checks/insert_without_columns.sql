declare
  var tab%rowtype;

  subtype my_type is tab%rowtype;
  v_my_type my_type;

  type t_tab is table of tab%rowtype index by binary_integer;
  v_tab t_tab;
begin
  insert into tab values (1); -- Noncompliant {{Specify the columns in this INSERT.}}
  
  insert into tab (col) values (1);

  insert into tab values var;

  insert into tab values var returning id into v_id;

  for r_t in (select * from mytable) loop
      insert into mytable values r_t;
  end loop;

  forall idx in v_tab.first .. v_tab.last
  insert into tab values v_tab(idx);

  insert into tab values v_tab(idx);

  insert into tab values v_my_type;

end;
