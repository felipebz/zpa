create table invoice_head (
  id number(10) constraint invoice_head_pk primary key,
  ref_code varchar2(20) not null,
  memo varchar2(200),
  lax_code varchar2(20) not null novalidate,
  off_code varchar2(20) constraint off_nn not null disable
);

create table invoice_line (
  head_id number(10),
  line_no number(5),
  amount number(12, 2) default 0 not null,
  constraint invoice_line_pk primary key (head_id, line_no)
);

create table invoice_copy as select * from invoice_head;

create table invoice_note (
  note_id number generated always as identity,
  body varchar2(200)
);

create table invoice_tag (
  tag_code varchar2(10) constraint invoice_tag_nn not null,
  label varchar2(30)
);

alter table invoice_tag disable constraint invoice_tag_nn;

create procedure check_invoices is
  n number;
begin
  select count(*) into n from invoice_head where id is null; -- Noncompliant {{Column "ID" is never null, so this condition is always false.}}
  select count(*) into n from invoice_head where ref_code is not null; -- Noncompliant {{Column "REF_CODE" is never null, so this condition is always true.}}
  select count(*) into n from invoice_head where memo is null;
  select count(*) into n from invoice_head where lax_code is null;
  select count(*) into n from invoice_head where off_code is null;
  select count(*) into n from invoice_line where line_no is null; -- Noncompliant
  select count(*) into n from invoice_line where amount is null; -- Noncompliant
  select count(*) into n from invoice_copy where ref_code is null;
  select count(*) into n from invoice_note where note_id is null; -- Noncompliant
  select count(*) into n from invoice_tag where tag_code is null;
end;
/
