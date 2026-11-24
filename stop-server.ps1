$tomcatHome = Join-Path $PSScriptRoot 'tomcat\apache-tomcat-10.1.62'
if (-not (Test-Path (Join-Path $tomcatHome 'bin\catalina.bat'))) { throw "Tomcat not found at $tomcatHome" }
$env:CATALINA_HOME = $tomcatHome
& (Join-Path $tomcatHome 'bin\catalina.bat') stop
