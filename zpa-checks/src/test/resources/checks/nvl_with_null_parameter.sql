begin
  var := nvl(x, null); -- Noncompliant {{This NVL does not have any effect. Fix the NULL parameter or remove this NVL.}}
--       ^^^^^^^^^^^^

  var := nvl(null, x); -- Noncompliant
--       ^^^^^^^^^^^^

  var := nvl(x, ''); -- Noncompliant {{This NVL does not have any effect. Fix the '' parameter or remove this NVL.}}
--       ^^^^^^^^^^

  var := nvl('', x); -- Noncompliant
--       ^^^^^^^^^^
  
  var := nvl(x, y);
  var := nvl(x, 'y');
  var := func(x, null);
  var := pack.func(x, null);
end;