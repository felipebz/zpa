begin
  select *
    from tab x; -- Noncompliant {{This statement has only one reference to the table "tab". Remove the alias "x" to improve the readability.}}
--           ^
    
  select *
    from tab xxxx, -- should be ignored because the accepted length is 4
         tab2;
end;
/