$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$tomcatHome = Join-Path $project 'tomcat\apache-tomcat-10.1.62'
$webContent = Join-Path $project 'WebContent'
$classes = Join-Path $webContent 'WEB-INF\classes'
$connector = Join-Path $webContent 'WEB-INF\lib\mysql-connector-j-26.10.0.jar'
$servletApi = Join-Path $tomcatHome 'lib\servlet-api.jar'
$compiler = Join-Path $tomcatHome 'lib\ecj-4.27.jar'

foreach ($required in @((Join-Path $tomcatHome 'bin\catalina.bat'), $connector, $servletApi, $compiler)) {
    if (-not (Test-Path $required)) { throw "Required project file not found: $required" }
}
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Java\jdk-27.0.1.0.3' }
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path $java)) { throw "Set JAVA_HOME to a JDK 11 or later. Current value: $env:JAVA_HOME" }
if (-not $env:CAMPUSPARK_DB_URL) { $env:CAMPUSPARK_DB_URL = 'jdbc:mysql://127.0.0.1:3306/college_parking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata' }
if (-not $env:CAMPUSPARK_DB_USER) { $env:CAMPUSPARK_DB_USER = 'root' }
if (-not $env:CAMPUSPARK_DB_PASSWORD) {
    $secure = Read-Host 'MySQL password' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { $env:CAMPUSPARK_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sourceFiles = Get-ChildItem (Join-Path $project 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object FullName
& $java -cp $compiler org.eclipse.jdt.internal.compiler.batch.Main -11 -encoding UTF-8 -classpath $servletApi -d $classes $sourceFiles
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed. See the compiler output above.' }

$deployment = Join-Path $tomcatHome 'webapps\CampusPark'
New-Item -ItemType Directory -Force -Path $deployment | Out-Null
Copy-Item -Path (Join-Path $webContent '*') -Destination $deployment -Recurse -Force
$env:CATALINA_HOME = $tomcatHome
Write-Host 'Starting CampusPark at http://localhost:8080/CampusPark/' -ForegroundColor Green
Write-Host 'Press Ctrl+C to stop Tomcat.'
& (Join-Path $tomcatHome 'bin\catalina.bat') run
