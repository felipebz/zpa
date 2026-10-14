# Oracle syntax probe for ZPA

`oracle-probe` runs isolated SQL and PL/SQL experiments against an Oracle test
schema while working on the ZPA grammar.

## Quick start

Start Oracle Free:

```bash
podman run -d --name oracle-free -p 1521:1521 \
  -e ORACLE_PASSWORD=oracle \
  -e APP_USER=zpa_probe \
  -e APP_USER_PASSWORD=zpa_probe \
  docker.io/gvenzl/oracle-free:full-faststart
```

This creates the `zpa_probe` user in `FREEPDB1` and publishes the listener on `localhost:1521`.

Create the probe configuration:

```bash
mkdir -p ~/.config/zpa
cp dev/oracle/oracle-probe.env.example ~/.config/zpa/oracle-probe.env
chmod 600 ~/.config/zpa/oracle-probe.env
```

The example configuration already matches the command above:

```bash
ZPA_ORACLE_USER=zpa_probe
ZPA_ORACLE_PASSWORD=zpa_probe
ZPA_ORACLE_CONNECT=localhost:1521/FREEPDB1
ORACLE_CONTAINER=oracle-free
CONTAINER_RUNTIME=podman
```

Check connectivity:

```bash
dev/oracle/oracle-probe --ping
```

Example output:

```text
STATUS=AVAILABLE
ORACLE_VERSION=Oracle AI Database 26ai Free Release ...
```

To use Docker instead, start the same image with `docker run` and set:

```bash
CONTAINER_RUNTIME=docker
```

## Running probes

Single statement:

```bash
printf '%s\n' 'select 1 from dual;' | dev/oracle/oracle-probe
```

Fixture plus statement:

```bash
cat <<'SQL' | dev/oracle/oracle-probe
create table t (
  id  number,
  grp number
);

select listagg(id) over (order by grp)
from t;
SQL
```

Accepted statements return:

```text
ORACLE_VERSION=...
RESULT=ACCEPTED
```

Rejected statements preserve Oracle's diagnostic and expose the first recognized
error code when available:

```text
ORACLE_VERSION=...
RESULT=REJECTED
ERROR_CODE=ORA-30487

select listagg(id) over (order by grp)
                         *
ERROR at line 1:
ORA-30487: ORDER BY not allowed here
```

PL/SQL objects created with compilation errors return `RESULT=COMPILE_ERROR`
and include `USER_ERRORS` rows prefixed with `ZPA_COMPILE_ERROR|`.

`ERROR_CODE`, when present, recognizes `ORA-xxxxx`, `PLS-xxxxx`, and
`SP2-xxxx`. It does not classify the error as syntactic or semantic.

## Isolation

Each probe starts from a clean `zpa_probe` schema and attempts to leave it
clean. Include every fixture required by the experiment in the same probe.

`reset-probe.sql` removes the common object types used by grammar experiments.
A cleanup failure is reported explicitly and returns exit code `3`.

## Existing Oracle installation

For an existing Oracle installation, use `bootstrap.sql` to create the probe
schema, then configure its credentials in
`~/.config/zpa/oracle-probe.env`.

If SQL*Plus is installed locally, leave `ORACLE_CONTAINER` unset and optionally
set:

```bash
ORACLE_CLIENT=sqlplus
```

## Windows (PowerShell)

`oracle-probe.ps1` is a PowerShell equivalent with the same configuration,
output, and exit codes. It works with Windows PowerShell 5.1 and PowerShell 7.

Create the configuration (same format and default location,
`%USERPROFILE%\.config\zpa\oracle-probe.env`):

```powershell
New-Item -ItemType Directory -Force "$HOME\.config\zpa" | Out-Null
Copy-Item dev\oracle\oracle-probe.env.example "$HOME\.config\zpa\oracle-probe.env"
```

Check connectivity (`--ping` and `-Ping` are equivalent):

```powershell
.\dev\oracle\oracle-probe.ps1 --ping
```

Run probes by piping SQL to stdin:

```powershell
'select 1 from dual;' | .\dev\oracle\oracle-probe.ps1

@'
create table t (
  id  number,
  grp number
);

select listagg(id) over (order by grp)
from t;
'@ | .\dev\oracle\oracle-probe.ps1

Get-Content probe.sql -Raw | .\dev\oracle\oracle-probe.ps1
```

Use an alternate configuration file:

```powershell
$env:ZPA_ORACLE_PROBE_CONFIG = 'C:\path\to\oracle-probe.env'
```

If script execution is blocked, run it with
`powershell -ExecutionPolicy Bypass -File dev\oracle\oracle-probe.ps1 ...`.

Differences from the bash script:

- probes are serialized with an exclusive lock file, by default
  `%TEMP%\zpa-oracle-probe-<user>.lock` (`flock` is not required);
- the client's stdout and stderr are captured separately and printed with
  stdout first, so their relative order is not preserved;
- SQL is sent to the client as UTF-8 with `LF` line endings;
- the exit code is in `$LASTEXITCODE`.

## Configuration

- `ZPA_ORACLE_USER`: probe schema user;
- `ZPA_ORACLE_PASSWORD`: probe schema password;
- `ZPA_ORACLE_CONNECT`: SQL*Plus connect target;
- `ORACLE_CONTAINER`: container name or ID; when set, SQL*Plus runs inside it;
- `CONTAINER_RUNTIME`: container CLI, default `podman`; set to `docker` for Docker;
- `ORACLE_CONTAINER_CLIENT`: SQL*Plus executable inside the container, default `sqlplus`;
- `ORACLE_CLIENT`: local client when `ORACLE_CONTAINER` is unset, default `sqlplus`;
- `ZPA_ORACLE_PROBE_CONFIG`: alternate configuration file path.

The container is never auto-discovered. When SQL*Plus runs inside the
recommended Oracle container, `localhost:1521/FREEPDB1` refers to the listener
inside that container.

## Exit codes

- `0`: accepted probe;
- `1`: rejected probe or PL/SQL compilation error;
- `2`: configuration, connectivity, reset-before, or version error;
- `3`: final cleanup failed.

## Using probe results

For ambiguous grammar:

1. identify the competing interpretations;
2. create minimal positive and negative cases;
3. include required fixtures in each probe;
4. compare Oracle results and diagnostics;
5. implement the narrowest ZPA grammar supported by the evidence;
6. add the discovered cases to the normal ZPA parser tests.

`RESULT=REJECTED` does not by itself mean syntax error. Oracle may reject a
statement during name resolution, type checking, privilege checks, or other
semantic processing.

Runtime results apply to the Oracle version reported by the probe unless other
evidence establishes broader version support.
