#Requires -Version 5.1
<#
.SYNOPSIS
  Run PEPS public-readiness automated tests (copy, resources, paste-mode layout, badges).
#>
$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RepoRoot

Write-Host "Running public-readiness + related GUI/model tests…"
& .\mvnw.cmd -q "-Dtest=PublicReadinessCopyTest,PublicReadinessResourcesTest,PublicReadinessDocsTest,AnonymizeBadgeLayoutTest,PlanSourcePasteModeLayoutTest,VisualWalkthroughChecklistTest,SampleExplainPlansTest,UiThemeContrastTest,Pev2MetricsTest" test
exit $LASTEXITCODE
