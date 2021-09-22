create procedure a is -- Noncompliant {{message}}
  i integer; -- Noncompliant {{message1}}
begin -- test method
    -- Noncompliant@+1 {{message2}}
    null;
    null;
    -- Noncompliant@-1 {{message3}}
    null; -- Noncompliant {{message4}} [[sc=9;ec=10;secondary=3,4,5]] bla bla bla
    null; -- Noncompliant
    -- Noncompliant@-4
    func(foo, -- Noncompliant [[sc=5;el=+1;ec=11]]
      bar);
    -- Noncompliant@+1 blabla
    null;
end;
/
