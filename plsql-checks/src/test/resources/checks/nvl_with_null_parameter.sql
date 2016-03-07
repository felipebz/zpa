begin
  var := nvl(x, null); -- Noncompliant {{This NVL does not have any effect. Fix the NULL parameter or remove this NVL.}} [[sc=10;ec=22]]
  var := nvl(null, x); -- Noncompliant [[sc=10;ec=22]]
  var := nvl(x, ''); -- Noncompliant {{This NVL does not have any effect. Fix the '' parameter or remove this NVL.}} [[sc=10;ec=20]]
  var := nvl('', x); -- Noncompliant [[sc=10;ec=20]]
  
  var := nvl(x, y);
  var := nvl(x, 'y');
  var := func(x, null);
  var := pack.func(x, null);
end;