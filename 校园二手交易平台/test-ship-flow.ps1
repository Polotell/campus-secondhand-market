[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:8080/api"

function Login($user, $pwd) {
    $body = @{ username = $user; password = $pwd } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$base/auth/login" -Method Post -ContentType "application/json" -Body $body
    if ($r.code -ne 0) { throw "login fail: $($r.message)" }
    return $r.data.token
}
function Hd($t) { @{ Authorization = "Bearer $t"; "Content-Type" = "application/json" } }

Write-Host "==== 1) login student01 + merchant01 ===="
$buyerToken = Login "student01" "admin123"
$merchToken = Login "merchant01" "admin123"
Write-Host "buyer token len=$($buyerToken.Length)  merchant token len=$($merchToken.Length)"

Write-Host "`n==== 2) pick an ON_SALE product belonging to merchant01 ====" 
$mp = Invoke-RestMethod -Uri "$base/merchant/products?status=ON_SALE&pageSize=20" -Method Get -Headers (Hd $merchToken)
$prod = @($mp.data.records) | Where-Object { $_.stock -gt 0 } | Select-Object -First 1
if (-not $prod) { throw "merchant01 has no ON_SALE product with stock>0" }
Write-Host "picked: id=$($prod.id) name=$($prod.name) stock=$($prod.stock) price=$($prod.discountPrice)"

Write-Host "`n==== 3) buyer add to cart ===="
$addBody = @{ productId = $prod.id; quantity = 1 } | ConvertTo-Json
$addRes = Invoke-RestMethod -Uri "$base/cart" -Method Post -Headers (Hd $buyerToken) -Body $addBody
if ($addRes.code -ne 0) { throw "add cart fail: $($addRes.message)" }
Write-Host "OK"

Write-Host "`n==== 4) get cart, take cartItemId ===="
$cartRes = Invoke-RestMethod -Uri "$base/cart" -Method Get -Headers (Hd $buyerToken)
$cartItem = @($cartRes.data.groups) | ForEach-Object { $_.items } | Where-Object { $_.productId -eq $prod.id } | Select-Object -First 1
$cid = $cartItem.id
Write-Host "cartItemId=$cid"

Write-Host "`n==== 5) checkout / create order ===="
$ckBody = @{ cartItemIds = @($cid); remark = "ship-test" } | ConvertTo-Json
$ckRes = Invoke-RestMethod -Uri "$base/orders" -Method Post -Headers (Hd $buyerToken) -Body $ckBody
if ($ckRes.code -ne 0) { throw "create order fail: $($ckRes.message)" }
$orderId = $ckRes.data.id
Write-Host "orderId=$orderId"

Write-Host "`n==== 6) buyer detail expects PAID ===="
$od = Invoke-RestMethod -Uri "$base/orders/$orderId" -Method Get -Headers (Hd $buyerToken)
Write-Host "status=$($od.data.status)  paidAt=$($od.data.paidAt)"
if ($od.data.status -ne "PAID") { throw "expected PAID but got $($od.data.status)" }

Write-Host "`n==== 7) merchant list (PAID) ===="
$ml = Invoke-RestMethod -Uri "$base/merchant/orders?status=PAID&pageSize=10" -Method Get -Headers (Hd $merchToken)
Write-Host "total=$($ml.data.total)  count=$(@($ml.data.records).Count)"
$has = @($ml.data.records) | Where-Object { $_.id -eq $orderId }
if (-not $has) { throw "merchant cannot see this order" }
Write-Host "merchant sees orderId=$orderId, buyerName=$($has.buyerName), shopName=$($has.shopName)"

Write-Host "`n==== 8) merchant ship ===="
$shipRes = Invoke-RestMethod -Uri "$base/merchant/orders/$orderId/ship" -Method Post -Headers (Hd $merchToken)
if ($shipRes.code -ne 0) { throw "ship fail: $($shipRes.message)" }
Write-Host "ship OK"

Write-Host "`n==== 9) detail expects SHIPPED + shippedAt + autoConfirmAt ===="
$od2 = Invoke-RestMethod -Uri "$base/orders/$orderId" -Method Get -Headers (Hd $buyerToken)
Write-Host "status=$($od2.data.status)"
Write-Host "shippedAt=$($od2.data.shippedAt)"
Write-Host "autoConfirmAt=$($od2.data.autoConfirmAt)"
if ($od2.data.status -ne "SHIPPED") { throw "expected SHIPPED" }
if (-not $od2.data.shippedAt) { throw "shippedAt should be set" }
if (-not $od2.data.autoConfirmAt) { throw "autoConfirmAt should be set" }

Write-Host "`n==== 10) duplicate ship should fail ===="
try {
    $r = Invoke-RestMethod -Uri "$base/merchant/orders/$orderId/ship" -Method Post -Headers (Hd $merchToken)
    Write-Host "code=$($r.code) message=$($r.message)"
    if ($r.code -eq 0) { throw "double ship should not succeed" }
} catch {
    $resp = $_.ErrorDetails.Message
    if ($resp) { Write-Host "rejected: $resp" } else { Write-Host "rejected with HTTP error" }
}

Write-Host "`n==== 11) buyer confirm receive SHIPPED -> RECEIVED ===="
$cfRes = Invoke-RestMethod -Uri "$base/orders/$orderId/confirm-receive" -Method Post -Headers (Hd $buyerToken)
if ($cfRes.code -ne 0) { throw "confirm fail: $($cfRes.message)" }

$od3 = Invoke-RestMethod -Uri "$base/orders/$orderId" -Method Get -Headers (Hd $buyerToken)
Write-Host "status=$($od3.data.status) receivedAt=$($od3.data.receivedAt) returnDeadline=$($od3.data.returnDeadline)"
if ($od3.data.status -ne "RECEIVED") { throw "expected RECEIVED" }
if (-not $od3.data.returnDeadline) { throw "returnDeadline should be set" }

Write-Host "`n==== 12) duplicate confirm should fail ===="
try {
    $r = Invoke-RestMethod -Uri "$base/orders/$orderId/confirm-receive" -Method Post -Headers (Hd $buyerToken)
    Write-Host "code=$($r.code) message=$($r.message)"
} catch {
    Write-Host "rejected: $($_.ErrorDetails.Message)"
}

Write-Host "`n[DONE] PAID -> SHIPPED -> RECEIVED  orderId=$orderId"
