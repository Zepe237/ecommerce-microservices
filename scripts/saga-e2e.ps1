<#
.SYNOPSIS
    End-to-end check of the payment saga, through the API gateway with a real Keycloak token.

.DESCRIPTION
    Scenario 1 - payment accepted : order CONFIRMED, stock deducted, cart emptied.
    Scenario 2 - payment declined : order CANCELLED, stock released, cart kept.

    Prerequisites: docker compose up -d, then every service started (dev profile, fake payment provider).
    Usage        : ./scripts/saga-e2e.ps1
#>
param(
    [string] $Gateway    = "http://localhost:8080",
    [string] $Keycloak   = "http://localhost:8180/realms/ecommerce",
    [string] $Username   = "jane.doe@example.com",
    [string] $Password   = "password",
    [long]   $CustomerId = 1,
    [string] $ProductId  = "PRD-0004",
    [int]    $TimeoutSec = 30
)

$ErrorActionPreference = "Stop"
$script:failures = 0

function Get-Token {
    $body = @{ grant_type = "password"; client_id = "ecommerce-dev-cli"; username = $Username; password = $Password }
    (Invoke-RestMethod -Method Post -Uri "$Keycloak/protocol/openid-connect/token" -Body $body).access_token
}

function Invoke-Api([string] $Method, [string] $Path, $Body = $null) {
    $params = @{ Method = $Method; Uri = "$Gateway$Path"; Headers = $script:headers }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Compress)
        $params.ContentType = "application/json"
    }
    Invoke-RestMethod @params
}

function Get-Stock    { Invoke-Api GET "/api/inventory/$ProductId" }
function Get-Order($id) { Invoke-Api GET "/api/orders/$id" }
function Get-CartSize { @((Invoke-Api GET "/api/carts/$CustomerId").items).Count }

# The saga is asynchronous (Kafka): poll until the condition holds or the timeout expires.
function Wait-Until([scriptblock] $Condition, [string] $What) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (& $Condition) { return $true }
        Start-Sleep -Milliseconds 500
    }
    Write-Host "    timeout while waiting for: $What" -ForegroundColor Yellow
    return $false
}

function Assert([bool] $Ok, [string] $Label) {
    if ($Ok) { Write-Host "  [OK]   $Label" -ForegroundColor Green }
    else     { Write-Host "  [FAIL] $Label" -ForegroundColor Red; $script:failures++ }
}

function Start-Order {
    Invoke-Api DELETE "/api/carts/$CustomerId" | Out-Null
    Invoke-Api POST "/api/carts/$CustomerId/items" @{ productId = $ProductId; quantity = 1 } | Out-Null
    $order = Invoke-Api POST "/api/orders" @{ customerId = $CustomerId }
    Assert ($order.status -eq "PAYMENT_PENDING") "order $($order.id) created as PAYMENT_PENDING"
    return $order
}

$script:headers = @{ Authorization = "Bearer $(Get-Token)" }
Write-Host "Token obtained for $Username"

# A fresh database has products and stock (dev seeders) but no customer: create Jane on first run.
try {
    Invoke-Api GET "/api/customers/$CustomerId" | Out-Null
} catch {
    $created = Invoke-Api POST "/api/customers" @{ name = "Jane Doe"; email = $Username }
    $CustomerId = $created.id
    Write-Host "Customer created with id $CustomerId"
}

# ---------------------------------------------------------------- Scenario 1
Write-Host "`nScenario 1 - payment accepted" -ForegroundColor Cyan
$before = Get-Stock
$order  = Start-Order

$reserved = Get-Stock
Assert ($reserved.reserved -eq $before.reserved + 1) "1 unit reserved (reserved: $($before.reserved) -> $($reserved.reserved))"

$payment = Invoke-Api POST "/api/payments" @{ orderId = $order.id; paymentMethod = "pm_card_visa" }
Assert ($payment.status -eq "SUCCEEDED") "payment SUCCEEDED"

Assert (Wait-Until { (Get-Order $order.id).status -eq "CONFIRMED" } "order CONFIRMED") "order CONFIRMED"
Assert (Wait-Until { $s = Get-Stock; $s.quantity -eq $before.quantity - 1 -and $s.reserved -eq $before.reserved } "stock committed") `
       "stock deducted for good (quantity: $($before.quantity) -> $($before.quantity - 1))"
Assert (Wait-Until { (Get-CartSize) -eq 0 } "cart emptied") "cart emptied"

# ---------------------------------------------------------------- Scenario 2
Write-Host "`nScenario 2 - payment declined" -ForegroundColor Cyan
$before = Get-Stock
$order  = Start-Order

$payment = Invoke-Api POST "/api/payments" @{ orderId = $order.id; paymentMethod = "pm_card_declined" }
Assert ($payment.status -eq "FAILED") "payment FAILED ($($payment.failureReason))"

Assert (Wait-Until { (Get-Order $order.id).status -eq "CANCELLED" } "order CANCELLED") "order CANCELLED"
Assert (Wait-Until { $s = Get-Stock; $s.quantity -eq $before.quantity -and $s.reserved -eq $before.reserved } "stock released") `
       "reserved unit released, quantity unchanged ($($before.quantity))"
Assert ((Get-CartSize) -eq 1) "cart kept, so the customer can retry"

# ---------------------------------------------------------------- Result
Write-Host ""
if ($script:failures -eq 0) {
    Write-Host "SAGA OK - all checks passed" -ForegroundColor Green
    exit 0
}
Write-Host "SAGA KO - $($script:failures) check(s) failed" -ForegroundColor Red
exit 1
