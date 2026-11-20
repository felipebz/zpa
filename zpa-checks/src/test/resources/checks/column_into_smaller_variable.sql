create procedure run_check(p_id number) is
  v_code  varchar2(5);
  v_tag   crate.code%type;
  v_short bin.short_code%type;
  v_qty   number(3);
  cursor c_crates is select code from crate;
begin
  select code into v_code from crate where id = p_id; -- Noncompliant {{Column "CODE" holds up to 10 characters, but "V_CODE" holds 5: a longer value raises ORA-06502.}}
  select code into v_tag from crate where id = p_id;
  select code into v_short from crate where id = p_id; -- Noncompliant
  select qty into v_qty from crate where id = p_id; -- Noncompliant {{Column "QTY" holds up to 6 digits before the point, but "V_QTY" holds 3: a larger value raises ORA-06502.}}
  open c_crates;
  fetch c_crates into v_code; -- Noncompliant
  close c_crates;
  update crate set qty = 0 where id = p_id returning code into v_code; -- Noncompliant
end;
/
create procedure run_kinds(p_id number) is
  v_pad   varchar2(5);
  v_wide  varchar2(5 byte);
  v_wchar varchar2(5 char);
  v_name  nvarchar2(5);
  v_hash  raw(8);
  v_price number(5, 1);
  v_cents number(7, 1);
  v_one   char;
  v_big   varchar2(20);
  v_int   integer;
  v_star  number(*, 2);
  v_num   number;
  v_text  clob;
  v_cnt   pls_integer;
  v_row   crate%rowtype;
  v_mb    varchar2(10 byte);
begin
  select pad into v_pad from crate where id = p_id; -- Noncompliant
  select wide into v_wide from crate where id = p_id;
  select wide into v_wchar from crate where id = p_id;
  select name into v_name from crate where id = p_id; -- Noncompliant
  select hash into v_hash from crate where id = p_id; -- Noncompliant {{Column "HASH" holds up to 16 bytes, but "V_HASH" holds 8: a longer value raises ORA-06502.}}
  select price into v_price from crate where id = p_id; -- Noncompliant
  -- A smaller scale only rounds.
  select price into v_cents from crate where id = p_id;
  select code into v_one from crate where id = p_id; -- Noncompliant
  -- The same size or larger, and sizes that are not known.
  select code into v_big from crate where id = p_id;
  select qty, price into v_int, v_star from crate where id = p_id;
  select qty, code, code into v_num, v_text, v_cnt from crate where id = p_id;
  select * into v_row from crate where id = p_id;
  select loose into v_qty_nowhere from crate where id = p_id;
  select upper(code), substr(code, 1, 5) into v_pad, v_pad from crate where id = p_id;
  -- Bytes against characters of the same number: not decided.
  select wide2 into v_mb from crate where id = p_id;
  -- A conversion, not a size.
  select code into v_star from crate where id = p_id;
  select qty into v_pad from crate where id = p_id;
end;
/
