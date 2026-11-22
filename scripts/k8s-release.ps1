<#
.SYNOPSIS
    Deploys a published version of the platform (images from GitHub Container Registry) to the kind cluster.

.DESCRIPTION
    Usage:  ./scripts/k8s-release.ps1 -Tag sha-1a2b3c4   (an exact, immutable version: recommended)
            ./scripts/k8s-release.ps1                    (the latest published version)
    Rollback: run it again with the previous tag (listed in the summary of each Release workflow run).
#>
param([string] $Tag = "latest")

$ErrorActionPreference = "Stop"
$services = "api-gateway","order-service","cart-service","product-service",
            "inventory-service","customer-service","notification-service","payment-service"

Write-Host "Deploying version '$Tag' from ghcr.io" -ForegroundColor Cyan

# Render the release overlay, pin every image to the requested tag, and apply the result.
kubectl kustomize k8s/release |
    ForEach-Object { $_ -replace '(ghcr\.io/zepe237/ecommerce-[a-z-]+):latest', "`$1:$Tag" } |
    kubectl apply -f -
if ($LASTEXITCODE -ne 0) { throw "kubectl apply failed" }

foreach ($s in $services) {
    kubectl -n ecommerce rollout status "deployment/$s" --timeout=300s
    if ($LASTEXITCODE -ne 0) { throw "rollout of $s failed" }
}
kubectl -n ecommerce get deployments -o custom-columns="NAME:.metadata.name,IMAGE:.spec.template.spec.containers[0].image,READY:.status.readyReplicas"
