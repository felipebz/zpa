begin
  v := 'a' || 'b'; -- Noncompliant [[sc=12;ec=13]]
  v := 'a'  ||  'b'; -- Noncompliant
  v := 'a' || 'b' || 'c'; -- Noncompliant
  v := 'a' || 'b' /* Noncompliant */ ||
       'c';
  v := 'a' || variable;
  v := variable || 'b';
  v := 'a' || variable || 'b';
  v := function_call('a') || 'b';
  v := ('a') || 'b';
  v := 'a' || /* comment */ 'b';
  v := 'a' /* comment */ || 'b';
  v := '' || 'a';
  v := 'a' || '';
  v := '' || '';
  v := q'[]' || 'a';
  v := N'' || 'a';
  v := N'' || N'a';
  v := N'a' || N'';
  v := N'' || N'';
  v := NQ'[]' || 'a';
  v := q'[a]' || q'[b]'; -- Noncompliant
  v := q'[a || b!]' || 'c'; -- Noncompliant
  v := q'[a]' || 'b'; -- Noncompliant
  v := N'a' || N'b'; -- Noncompliant
  v := NQ'[a]' || NQ'[b]'; -- Noncompliant
  v := 'a' || N'b';
  v := N'a' || 'b';
  v := 'I''m ' || 'here'; -- Noncompliant
  v := concat('a', 'b');
  v := 'a' ||
       'b';
  v := 'a'
       || 'b';
end;
