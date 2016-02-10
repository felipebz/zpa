begin
  var := nvl(x, null); -- Noncompliant {{Remove the NULL parameter of this NVL.}} [[sc=10;ec=22]]
  var := nvl(null, x); -- Noncompliant [[sc=10;ec=22]]
  var := nvl(x, ''); -- Noncompliant {{Remove the '' parameter of this NVL.}} [[sc=10;ec=20]]
  var := nvl('', x); -- Noncompliant [[sc=10;ec=20]]
  
  var := nvl(x, y);
  var := nvl(x, 'y');
  var := func(x, null);
  var := pack.func(x, null);
end;