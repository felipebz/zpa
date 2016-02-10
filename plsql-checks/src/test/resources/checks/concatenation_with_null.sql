begin
  var := 'a'||null; -- Noncompliant {{Review this concatenation with NULL value.}} [[sc=15;ec=19]]
  var := 'a'||''; -- Noncompliant [[sc=15;ec=17]]
  var := 'a'||'1';
end;