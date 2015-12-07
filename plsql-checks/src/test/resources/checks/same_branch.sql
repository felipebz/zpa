begin
  if x then
    var := 1;
  elsif y then
    var := 1; -- violation
  end if;
  
  if x then
    var := 1;
  else
    var := 1; -- violation
  end if;
end;