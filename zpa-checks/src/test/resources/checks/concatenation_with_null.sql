begin
  var := 'a'||null; -- Noncompliant {{Review this concatenation with NULL value.}}
--            ^^^^

  var := 'a'||''; -- Noncompliant
--            ^^

  var := 'a'||'1';
end;