begin
  -- invalid usage
  foo := (x != a);
  foo := (x ^= a);
  foo := (x ~= a);
  
  -- valid usage
  foo := (x <> a);
end;