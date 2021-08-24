SELECT name FROM t
  WHERE JSON_EXISTS(name, '$[1].middle');