begin
  show_lov('foo');
  show_lov('invalid'); -- Noncompliant {{Invalid reference to the object "invalid" in this SHOW_LOV call.}}
--         ^^^^^^^^^
end;