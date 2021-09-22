begin
  var := x like 'a'; -- Noncompliant
--       ^^^^^^^^^^
  var := x like 'a\%' escape '\'; -- Noncompliant
  var := x like 'a\_' escape '\';  -- Noncompliant
  var := x like '%a';
  var := x like 'a_';
  var := x like '%a\%' escape '\';
  var := x like '%a\_' escape '\';
  var := x like y;
  var := x like y escape z;
  var := x like x escape '\';
end;
