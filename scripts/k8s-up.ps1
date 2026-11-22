<#
.SYNOPSIS
    Builds every service, loads the images into a local kind cluster and deploys the platform.

.DESCRIPTION
    Prerequisites: Docker Desktop running, docker compose up -d (infrastructure), kind and kubectl installed.
    Usage:  ./scripts/k8s-up.ps1          (full build and deploy)
            ./scripts/k8s-up.ps1 -SkipBuild   (redeploy the existing images)
    Remove: kind delete cluster --name ecommerce
#>
param([switch] $SkipBuild)

$ErrorActionPreference = "Stop"
$cluster  = "ecommerce"
$services = "api-gateway","order-service","cart-service","product-service",
            "inventory-service","customer-service","notification-service","payment-service"

function Step([string] $text) { Write-Host "`n==> $text" -ForegroundColor Cyan }
function Check([string] $what) { if ($LASTEXITCODE -ne 0) { throw "$what failed (exit code $LASTEXITCODE)" } }

# 1. Cluster
if (-not ((kind get clusters) -contains $cluster)) {
    Step "Creating the kind cluster '$cluster'"
    kind create cluster --config k8s/kind-config.yaml; Check "kind create cluster"
} else {
    Step "Cluster '$cluster' already exists"
}

if (-not $SkipBuild) {
    # 2. Jars: the tests already ran in the CI and locally, skip them here.
    Step "Building the jars"
    ./mvnw -B -q -DskipTests package; Check "Maven package"

    # 3. Images, then copy them into the cluster node (kind has no access to the local Docker images otherwise).
    foreach ($s in $services) {
        Step "Building image ecommerce/${s}:local"
        docker build -q --build-arg MODULE=$s -t "ecommerce/${s}:local" .; Check "docker build $s"
    }
    Step "Loading the images into the cluster"
    kind load docker-image --name $cluster ($services | ForEach-Object { "ecommerce/${_}:local" }); Check "kind load"
}

# 4. Deploy (kustomize is built into kubectl: -k reads k8s/kustomization.yaml)
Step "Applying the manifests"
kubectl apply -k k8s; Check "kubectl apply"
# Same tag 'local' after a rebuild: force the pods to restart on the new image.
kubectl -n ecommerce rollout restart deployment | Out-Null

Step "Waiting for every deployment to be ready"
foreach ($s in $services) {
    kubectl -n ecommerce rollout status "deployment/$s" --timeout=300s; Check "rollout $s"
}

kubectl -n ecommerce get pods -o wide
Write-Host "`nPlatform ready on http://localhost:8080 - run ./scripts/saga-e2e.ps1" -ForegroundColor Green
