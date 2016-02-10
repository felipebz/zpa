begin
  var := (x = true); -- Noncompliant {{Remove the literal "TRUE" of this comparison.}}
  var := (x != true); -- Noncompliant
  var := (x = false); -- Noncompliant {{Remove the literal "FALSE" of this comparison.}} 
  var := (x != false); -- Noncompliant
  var := (true = true); -- Noncompliant
  
  var := (x = y);
end;