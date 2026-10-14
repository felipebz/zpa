<#
.SYNOPSIS
  PowerShell equivalent of the bash `oracle-probe` wrapper.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
  [switch]$Ping,
  [Alias('h')][switch]$Help,
  [Parameter(ValueFromRemainingArguments = $true)][string[]]$Rest,
  # SQL piped from PowerShell (`'...' | .\oracle-probe.ps1`). The script body is
  # the end block, so `$input` holds every piped line.
  [Parameter(ValueFromPipeline = $true)][AllowEmptyString()][string]$InputObject
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$Root = $PSScriptRoot
$Config = if ($env:ZPA_ORACLE_PROBE_CONFIG) { $env:ZPA_ORACLE_PROBE_CONFIG } else { Join-Path $HOME '.config/zpa/oracle-probe.env' }
$OracleClientDefault = 'sqlplus'
$ContainerRuntimeDefault = 'podman'
$OracleContainerClientDefault = 'sqlplus'
$LockFileDefault = Join-Path ([System.IO.Path]::GetTempPath()) ("zpa-oracle-probe-{0}.lock" -f [Environment]::UserName)

function Show-Usage {
@'
Usage:
  oracle-probe.ps1 --ping
  oracle-probe.ps1

Reads a self-contained SQL/PLSQL probe from stdin and executes it against the
configured Oracle test schema.

Options:
  --ping   Validate connectivity and print the Oracle version.
  -h       Show this help.

Configuration file (default):
  ~/.config/zpa/oracle-probe.env

Set ORACLE_CONTAINER in that file to execute SQL*Plus inside an existing
Oracle container. Set CONTAINER_RUNTIME to `podman` or `docker`. Leave
ORACLE_CONTAINER unset to use a local client.

Override with:
  $env:ZPA_ORACLE_PROBE_CONFIG = 'C:\path\to\oracle-probe.env'

Normal probe executions are serialized because they share and reset the same
probe schema. --ping does not acquire the probe lock.
'@
}

# Accept bash-style long options too (--ping, --help).
foreach ($arg in @($Rest | Where-Object { $_ })) {
  switch -CaseSensitive ($arg) {
    '--ping' { $Ping = $true }
    { $_ -in '--help', '-h' } { $Help = $true }
    default {
      [Console]::Error.WriteLine("Unknown argument: $arg")
      [Console]::Error.WriteLine((Show-Usage))
      exit 2
    }
  }
}

if ($Help) { Show-Usage; exit 0 }

if (-not (Test-Path -LiteralPath $Config -PathType Leaf)) {
  [Console]::Error.WriteLine("Oracle probe configuration not found: $Config")
  [Console]::Error.WriteLine('Copy oracle-probe.env.example and adjust the values.')
  exit 2
}

# Minimal KEY=VALUE parser (comments, blank lines, optional `export`, quotes).
# Values from the file override the environment, as `source` does in bash.
$Settings = @{}
foreach ($name in 'ZPA_ORACLE_USER', 'ZPA_ORACLE_PASSWORD', 'ZPA_ORACLE_CONNECT', 'ORACLE_CONTAINER',
  'ORACLE_CONTAINER_CLIENT', 'CONTAINER_RUNTIME', 'ORACLE_CLIENT', 'SQLPLUS_BIN', 'SQLCL_BIN',
  'ZPA_ORACLE_PROBE_LOCK_FILE') {
  $v = [Environment]::GetEnvironmentVariable($name)
  if ($v) { $Settings[$name] = $v }
}
foreach ($line in Get-Content -LiteralPath $Config) {
  if ($line -match '^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
    $value = $Matches[2].Trim()
    if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") { $value = $Matches[1] }
    else { $value = ($value -replace '\s+#.*$', '') }
    $Settings[$Matches[1]] = $value
  }
}

function Get-Setting([string]$Name, [string]$Default = '') {
  if ($Settings.ContainsKey($Name) -and $Settings[$Name]) { return [string]$Settings[$Name] }
  return $Default
}

foreach ($required in 'ZPA_ORACLE_USER', 'ZPA_ORACLE_PASSWORD', 'ZPA_ORACLE_CONNECT') {
  if (-not (Get-Setting $required)) {
    [Console]::Error.WriteLine("$required is required")
    exit 1
  }
}

$ZpaOracleUser = Get-Setting 'ZPA_ORACLE_USER'
$ZpaOraclePassword = Get-Setting 'ZPA_ORACLE_PASSWORD'
$ZpaOracleConnect = Get-Setting 'ZPA_ORACLE_CONNECT'
$OracleContainer = Get-Setting 'ORACLE_CONTAINER'
$OracleContainerClient = Get-Setting 'ORACLE_CONTAINER_CLIENT' $OracleContainerClientDefault
$ContainerRuntime = Get-Setting 'CONTAINER_RUNTIME' $ContainerRuntimeDefault
$OracleClient = Get-Setting 'ORACLE_CLIENT' (Get-Setting 'SQLPLUS_BIN' (Get-Setting 'SQLCL_BIN' $OracleClientDefault))
$LockFile = Get-Setting 'ZPA_ORACLE_PROBE_LOCK_FILE' $LockFileDefault

function Resolve-Executable([string]$Name) {
  $cmd = Get-Command -Name $Name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($cmd) { return $cmd.Source }
  return $null
}

if ($OracleContainer) {
  $Exe = Resolve-Executable $ContainerRuntime
  if (-not $Exe) { [Console]::Error.WriteLine("Container runtime executable not found: $ContainerRuntime"); exit 2 }
  $ExeArgs = @('exec', '-i', $OracleContainer, $OracleContainerClient, '-S', '-L', '/nolog')
} else {
  $Exe = Resolve-Executable $OracleClient
  if (-not $Exe) { [Console]::Error.WriteLine("Oracle client executable not found: $OracleClient"); exit 2 }
  $ExeArgs = @('-S', '-L', '/nolog')
}

$ResetSql = Join-Path $Root 'reset-probe.sql'

# Runs the client with $Script on stdin; returns combined output and exit code.
# stdout and stderr are captured separately and concatenated (stdout first).
function Invoke-OracleClient([string]$Script) {
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $Exe
  $psi.Arguments = ($ExeArgs | ForEach-Object {
      if ($_ -match '[\s"]') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ }
    }) -join ' '
  $psi.UseShellExecute = $false
  $psi.RedirectStandardInput = $true
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $utf8 = New-Object System.Text.UTF8Encoding($false)
  $psi.StandardOutputEncoding = $utf8
  $psi.StandardErrorEncoding = $utf8

  $proc = [System.Diagnostics.Process]::Start($psi)
  $outTask = $proc.StandardOutput.ReadToEndAsync()
  $errTask = $proc.StandardError.ReadToEndAsync()
  $writer = New-Object System.IO.StreamWriter($proc.StandardInput.BaseStream, $utf8)
  $writer.NewLine = "`n"
  try { $writer.Write(($Script -replace "`r`n", "`n")) ; $writer.Flush() } catch [System.IO.IOException] { }
  finally { try { $writer.Close() } catch { } }
  $proc.WaitForExit()
  $text = $outTask.Result + $errTask.Result
  [pscustomobject]@{ Output = ($text -replace "`r`n", "`n").TrimEnd("`n"); Status = $proc.ExitCode }
}

function Split-Lines([string]$Text) { if ($null -eq $Text -or $Text -eq '') { @() } else { $Text -split "`n" } }

function Get-TrimmedOuterBlankLines([string]$Text) {
  $lines = @(Split-Lines $Text)
  $first = 0; $last = $lines.Count - 1
  while ($first -le $last -and $lines[$first] -match '^\s*$') { $first++ }
  while ($last -ge $first -and $lines[$last] -match '^\s*$') { $last-- }
  if ($first -gt $last) { return '' }
  return ($lines[$first..$last] -join "`n")
}

function Get-Section([string]$Text, [string]$Begin, [string]$End) {
  $capture = $false
  $result = foreach ($l in Split-Lines $Text) {
    if ($l -ceq $Begin) { $capture = $true; continue }
    if ($l -ceq $End) { break }
    if ($capture) { $l }
  }
  return (@($result) -join "`n")
}

function Get-Version([string]$Text) {
  foreach ($l in Split-Lines (Get-Section $Text 'ZPA_VERSION_BEGIN' 'ZPA_VERSION_END')) {
    if ($l -notmatch '^\s*$') { return $l.Trim() }
  }
  return ''
}

function Test-Line([string]$Text, [string]$Pattern) {
  foreach ($l in Split-Lines $Text) { if ($l -cmatch $Pattern) { return $true } }
  return $false
}

function Write-ErrorCode([string]$Text) {
  $m = [regex]::Match($Text, '(ORA|PLS)-[0-9]{5}|SP2-[0-9]{4}')
  if ($m.Success) { Write-Output "ERROR_CODE=$($m.Value)" }
}

function Write-Text([string]$Text) { foreach ($l in Split-Lines $Text) { Write-Output $l } }

$SessionPrelude = @"
set echo off
set verify off
set feedback off
set heading off
set pagesize 0
set linesize 32767
set trimspool on
set serveroutput on size unlimited
set sqlblanklines on
set define off
whenever oserror exit failure
whenever sqlerror exit failure
connect $ZpaOracleUser/"$ZpaOraclePassword"@$ZpaOracleConnect

"@

$VersionSql = @'
prompt ZPA_VERSION_BEGIN
select banner
  from v$version
 where lower(banner) like '%database%'
   and rownum = 1;
prompt ZPA_VERSION_END

'@

function Get-ResetSql {
  $text = [System.IO.File]::ReadAllText($ResetSql)
  if (-not $text.EndsWith("`n")) { $text += "`n" }
  return $text
}

if ($Ping) {
  $r = Invoke-OracleClient ($SessionPrelude + $VersionSql + "exit success`n")
  if ($r.Status -ne 0) {
    Write-Output 'STATUS=UNAVAILABLE'
    Write-Text $r.Output
    exit 1
  }
  $version = Get-Version $r.Output
  if (-not $version) {
    Write-Output 'STATUS=UNAVAILABLE'
    Write-Output 'Unable to determine Oracle version from V$VERSION.'
    Write-Text $r.Output
    exit 1
  }
  Write-Output 'STATUS=AVAILABLE'
  Write-Output "ORACLE_VERSION=$version"
  exit 0
}

$Piped = @($input)
if ($Piped.Count -gt 0) {
  $ProbeSql = ($Piped | ForEach-Object { [string]$_ }) -join "`n"
} elseif ([Console]::IsInputRedirected) {
  # Invoked as an external process (`powershell -File ... < probe.sql`).
  $ProbeSql = [Console]::In.ReadToEnd()
} else {
  $ProbeSql = ''
}
if ($ProbeSql -match '^\s*$') {
  [Console]::Error.WriteLine('No SQL probe received on stdin.')
  exit 2
}
$ProbeSql = ($ProbeSql -replace "`r`n", "`n").TrimEnd("`n") + "`n"

# Exclusive lock for the whole mutable lifecycle (reset-before .. reset-after).
# Acquired only after stdin was read and validated; --ping never takes it.
$LockDir = Split-Path -Parent $LockFile
if ($LockDir -and -not (Test-Path -LiteralPath $LockDir -PathType Container)) {
  try { New-Item -ItemType Directory -Path $LockDir -Force | Out-Null }
  catch { [Console]::Error.WriteLine("Unable to create Oracle probe lock directory: $LockDir"); exit 2 }
}

$LockStream = $null
while (-not $LockStream) {
  try {
    $LockStream = [System.IO.File]::Open($LockFile, 'OpenOrCreate', 'ReadWrite', 'None')
  } catch [System.IO.IOException] {
    # Held by another probe: wait like `flock -x`.
    Start-Sleep -Milliseconds 200
  } catch {
    [Console]::Error.WriteLine("Unable to open Oracle probe lock file: $LockFile")
    exit 2
  }
}

try {
  $Script = $SessionPrelude +
  "prompt ZPA_RESET_BEFORE_BEGIN`n" + (Get-ResetSql) + "prompt ZPA_RESET_BEFORE_END`n" +
  $VersionSql + @'
variable zpa_oracle_probe_exit_code number
begin
  :zpa_oracle_probe_exit_code := 0;
end;
/

prompt ZPA_PROBE_BEGIN

'@ + $ProbeSql + @'

declare
  l_errors number;
begin
  select count(*)
    into l_errors
    from user_errors;

  if l_errors > 0 then
    :zpa_oracle_probe_exit_code := 1;

    for r in (
      select type,
             name,
             line,
             position,
             text
        from user_errors
       order by name, sequence
    ) loop
      dbms_output.put_line(
        'ZPA_COMPILE_ERROR|' ||
        r.type || '|' ||
        r.name || '|' ||
        r.line || '|' ||
        r.position || '|' ||
        replace(replace(r.text, chr(10), ' '), chr(13), ' ')
      );
    end loop;
  end if;
end;
/

prompt ZPA_PROBE_END
prompt ZPA_CLEANUP_BEGIN
whenever sqlerror continue

'@ + (Get-ResetSql) + @'
prompt ZPA_CLEANUP_END
exit :zpa_oracle_probe_exit_code

'@

  $r = Invoke-OracleClient $Script
  $RawOutput = $r.Output
  $Status = $r.Status

  # Failures before the probe starts are environment/setup failures.
  $stage = $null
  if (-not (Test-Line $RawOutput '^ZPA_RESET_BEFORE_BEGIN$')) { $stage = 'CONNECT' }
  elseif (-not (Test-Line $RawOutput '^ZPA_RESET_BEFORE_END$')) { $stage = 'RESET_BEFORE' }
  if ($stage) {
    Write-Output 'RESULT=ENVIRONMENT_ERROR'
    Write-Output "STAGE=$stage"
    Write-ErrorCode $RawOutput
    Write-Text (Get-TrimmedOuterBlankLines $RawOutput)
    exit 2
  }

  $Version = Get-Version $RawOutput
  if (-not $Version) {
    Write-Output 'RESULT=ENVIRONMENT_ERROR'
    Write-Output 'STAGE=VERSION'
    Write-ErrorCode $RawOutput
    Write-Text (Get-TrimmedOuterBlankLines $RawOutput)
    exit 2
  }

  if (-not (Test-Line $RawOutput '^ZPA_PROBE_BEGIN$')) {
    Write-Output "ORACLE_VERSION=$Version"
    Write-Output 'RESULT=ENVIRONMENT_ERROR'
    Write-Output 'STAGE=PROBE_SETUP'
    Write-ErrorCode $RawOutput
    Write-Text (Get-TrimmedOuterBlankLines $RawOutput)
    exit 2
  }

  $ProbeOutput = Get-TrimmedOuterBlankLines (Get-Section $RawOutput 'ZPA_PROBE_BEGIN' 'ZPA_PROBE_END')

  $Result = 'ACCEPTED'
  if (Test-Line $ProbeOutput '^ZPA_COMPILE_ERROR\|') { $Result = 'COMPILE_ERROR' }
  elseif ($Status -ne 0) { $Result = 'REJECTED' }

  Write-Output "ORACLE_VERSION=$Version"
  Write-Output "RESULT=$Result"
  if ($Result -ne 'ACCEPTED') { Write-ErrorCode $ProbeOutput }
  if ($ProbeOutput) { Write-Output ''; Write-Text $ProbeOutput }

  $CleanupDone = $false
  if (Test-Line $RawOutput '^ZPA_CLEANUP_END$') {
    $CleanupDone = $true
    $CleanupOutput = Get-TrimmedOuterBlankLines (Get-Section $RawOutput 'ZPA_CLEANUP_BEGIN' 'ZPA_CLEANUP_END')
    if ((Test-Line $CleanupOutput '^ZPA_CLEANUP_WARNING\|') -or $CleanupOutput.Contains('ORA-20990')) {
      Write-Output ''
      Write-Output 'CLEANUP=FAILED'
      Write-Text $CleanupOutput
      exit 3
    }
  }

  # Hard SQL/OS errors exit before in-session cleanup; reset in a new session.
  if (-not $CleanupDone) {
    $c = Invoke-OracleClient ($SessionPrelude + (Get-ResetSql) + "exit success`n")
    if ($c.Status -ne 0) {
      Write-Output ''
      Write-Output 'CLEANUP=FAILED'
      Write-Text (Get-TrimmedOuterBlankLines $c.Output)
      exit 3
    }
  }

  switch ($Result) {
    'ACCEPTED' { exit 0 }
    { $_ -in 'REJECTED', 'COMPILE_ERROR' } { exit 1 }
    default { exit 2 }
  }
}
finally {
  $LockStream.Dispose()
}
