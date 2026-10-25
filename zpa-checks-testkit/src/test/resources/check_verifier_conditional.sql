begin -- Noncompliant@+4 {{null statement}}
  $if $$flag $then
    x := 1;
  $else
    null;
  $end
end;
/
