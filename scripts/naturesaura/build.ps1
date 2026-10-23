param(
    [string]$InputJar = 'C:\Users\bruns\Downloads\NaturesAura-39.4-throttled.jar',
    [string]$OutputJar = 'build\naturesaura\NaturesAura-39.4-throttled-chunk-threadsafe.jar'
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location -LiteralPath $taskRoot
$javaTools = 'C:\Program Files\Java\jdk-17.0.18\bin'
$dependencyRoot = 'C:\Users\bruns\.gradle\caches\modules-2\files-2.1'
$asmJar = Get-ChildItem -LiteralPath "$dependencyRoot\org.ow2.asm\asm\9.5" -Recurse -Filter 'asm-9.5.jar' | Select-Object -First 1 -ExpandProperty FullName
$fastutilJar = Get-ChildItem -LiteralPath "$dependencyRoot\it.unimi.dsi\fastutil\8.5.9" -Recurse -Filter 'fastutil-8.5.9.jar' | Select-Object -First 1 -ExpandProperty FullName
if (!$asmJar -or !$fastutilJar) { throw 'Missing cached ASM 9.5 or FastUtil 8.5.9.' }
New-Item -ItemType Directory -Force 'build\naturesaura\classes' | Out-Null
New-Item -ItemType Directory -Force (Split-Path -Parent $OutputJar) | Out-Null
& "$javaTools\javac.exe" --release 17 -cp "$asmJar;$fastutilJar" -d 'build\naturesaura\classes' 'scripts\naturesaura\PatchAuraChunkIndex.java' 'src\compat\naturesaura\java\de\ellpeck\naturesaura\compat\AuraChunkIndexGuard.java'
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
& "$javaTools\java.exe" -Xverify:all -cp "build\naturesaura\classes;$asmJar;$fastutilJar" PatchAuraChunkIndex $InputJar $OutputJar 'build\naturesaura\classes\de\ellpeck\naturesaura\compat\AuraChunkIndexGuard.class'
if ($LASTEXITCODE -ne 0) { throw 'Patch failed.' }
