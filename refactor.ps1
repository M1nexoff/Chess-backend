$src = "src\main\java\com\chessapp\server"

$mappings = @(
    @{ Old="data\model\enums"; New="domain\enums"; OldPkg="com.chessapp.server.data.model.enums"; NewPkg="com.chessapp.server.domain.enums" },
    @{ Old="data\model"; New="domain\model"; OldPkg="com.chessapp.server.data.model"; NewPkg="com.chessapp.server.domain.model" },
    @{ Old="repository"; New="infrastructure\persistence"; OldPkg="com.chessapp.server.repository"; NewPkg="com.chessapp.server.infrastructure.persistence" },
    @{ Old="security"; New="infrastructure\security"; OldPkg="com.chessapp.server.security"; NewPkg="com.chessapp.server.infrastructure.security" },
    @{ Old="controller"; New="presentation\rest"; OldPkg="com.chessapp.server.controller"; NewPkg="com.chessapp.server.presentation.rest" },
    @{ Old="websocket"; New="presentation\websocket"; OldPkg="com.chessapp.server.websocket"; NewPkg="com.chessapp.server.presentation.websocket" },
    @{ Old="service"; New="application\service"; OldPkg="com.chessapp.server.service"; NewPkg="com.chessapp.server.application.service" },
    @{ Old="config"; New="infrastructure\config"; OldPkg="com.chessapp.server.config"; NewPkg="com.chessapp.server.infrastructure.config" },
    @{ Old="utils"; New="infrastructure\utils"; OldPkg="com.chessapp.server.utils"; NewPkg="com.chessapp.server.infrastructure.utils" }
)

foreach ($map in $mappings) {
    $newDir = Join-Path $src $map.New
    $oldDir = Join-Path $src $map.Old
    
    if (-not (Test-Path $newDir)) {
        New-Item -ItemType Directory -Force -Path $newDir | Out-Null
    }
    
    if (Test-Path $oldDir) {
        Write-Host "Moving items from $oldDir to $newDir"
        Get-ChildItem -Path $oldDir -File | Move-Item -Destination $newDir -Force
    }
}

# Recursively update package and import statements in all Java files
$allJavaFiles = Get-ChildItem -Path $src -Filter "*.java" -Recurse
foreach ($file in $allJavaFiles) {
    Write-Host "Processing $($file.FullName)"
    $content = Get-Content $file.FullName -Raw
    
    # Process replacements
    foreach ($map in $mappings) {
        # Using regex replace to target either package or import exactly
        $oldEscaped = [Regex]::Escape($map.OldPkg)
        $content = [Regex]::Replace($content, "\b$oldEscaped\b", $map.NewPkg)
    }
    
    Set-Content -Path $file.FullName -Value $content -NoNewline
}

Write-Host "Refactoring step 1 completed successfully."
