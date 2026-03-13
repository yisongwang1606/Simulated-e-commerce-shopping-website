param(
    [switch]$SkipStart
)

$ErrorActionPreference = "Stop"

Write-Host "Stopping project containers and removing attached volumes..."
docker compose down -v --remove-orphans

Write-Host "Pruning unused Docker images, build cache, networks, and volumes..."
docker system prune -a --volumes -f

if (-not $SkipStart) {
    Write-Host "Rebuilding the latest project images and starting the stack..."
    docker compose up --build -d
}

Write-Host "Done."
