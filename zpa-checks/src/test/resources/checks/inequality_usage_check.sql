begin
  foo := (x != a); -- Noncompliant {{Replace "!=" by "<>".}}
  foo := (x ^= a); -- Noncompliant {{Replace "^=" by "<>".}}
  foo := (x ~= a); -- Noncompliant {{Replace "~=" by "<>".}}
  
  -- valid usage
  foo := (x <> a);
end;