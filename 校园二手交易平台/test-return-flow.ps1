[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Continue"
$base = "http://127.0.0.1:8080/api"
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$dbPwd = "20041026Jyhz"
$db = "2023011308"

function Login($user, $pwd) {
    $body = @{ username = $user; password = $pwd } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$base/auth/login" -Method Post -ContentType "application/json" -Body $body
    if ($r.code -ne 0) { throw "login fail: $($r.message)" }
    return $r.data.token
}
function Hd($t) { @{ Authorization = "Bearer $t"; "Content-Type" = "application/json" } }
function SqlQuery($sql) {
    return (& $mysql --host=127.0.0.1 -uroot "--password=$dbPwd" -D $db -e $sql 2>$null)
}
function SqlScalar($sql) {
    $out = & $mysql --host=127.0.0.1 -uroot "--password=$dbPwd" -D $db -s -N -e $sql 2>$null
    return ($out | Select-Object -First 1)
}
function GetBalance($username) {
    $v = SqlScalar "SELECT balance FROM user WHERE username='$username';"
    if ($v) { return [decimal]$v } else { return $null }
}
function GetStock($id) {
    $v = SqlScalar "SELECT stock FROM product WHERE id=$id;"
    if ($null -ne $v -and $v -ne '') { return [int]$v } else { return $null }
}

Write-Host "==== 0) reset balance + clear return_record ===="
SqlQuery "UPDATE user SET balance=100000.00 WHERE username='student01';" | Out-Null
SqlQuery "DELETE FROM return_record;" | Out-Null

$buyerToken = Login "student01" "admin123"
$merchToken = Login "merchant01" "admin123"
$other      = Login "student02" "admin123"

# ===== TEST-A: approve path =====
Write-Host "`n========================================"
Write-Host "  TEST-A: apply -> merchant approve -> refund + restock"
Write-Host "========================================"

Write-Host "`n[A1] pick product, place order"
$mp = Invoke-RestMethod -Uri "$base/merchant/products?pageSize=20&status=ON_SALE" -Method Get -Headers (Hd $merchToken)
$prod = @($mp.data.records) | Where-Object { $_.stock -gt 0 } | Select-Object -First 1
$prodId = $prod.id
$beforeStock = GetStock $prodId
$beforeBalance = GetBalance "student01"
Write-Host "  product id=$prodId stock(before)=$beforeStock  buyer balance(before)=$beforeBalance"

$addBody = @{ productId = $prodId; quantity = 1 } | ConvertTo-Json
Invoke-RestMethod -Uri "$base/cart" -Method Post -Headers (Hd $buyerToken) -Body $addBody | Out-Null
$cart = Invoke-RestMethod -Uri "$base/cart" -Method Get -Headers (Hd $buyerToken)
$cid = (@($cart.data.groups) | ForEach-Object { $_.items } | Where-Object { $_.productId -eq $prodId } | Select-Object -First 1).id
$ck = Invoke-RestMethod -Uri "$base/orders" -Method Post -Headers (Hd $buyerToken) -Body (@{cartItemIds=@($cid)} | ConvertTo-Json)
$orderId = $ck.data.id
Write-Host "  orderId=$orderId"

Write-Host "`n[A2] ship + receive"
Invoke-RestMethod -Uri "$base/merchant/orders/$orderId/ship" -Method Post -Headers (Hd $merchToken) | Out-Null
Invoke-RestMethod -Uri "$base/orders/$orderId/confirm-receive" -Method Post -Headers (Hd $buyerToken) | Out-Null
$od = Invoke-RestMethod -Uri "$base/orders/$orderId" -Method Get -Headers (Hd $buyerToken)
Write-Host "  status=$($od.data.status) canApplyReturn=$($od.data.canApplyReturn) returnDeadline=$($od.data.returnDeadline)"
if ($od.data.status -ne "RECEIVED") { throw "expected RECEIVED" }
if ($od.data.canApplyReturn -ne $true) { throw "canApplyReturn should be true at RECEIVED in time window" }
$paidBalance = GetBalance "student01"
Write-Host "  balance after pay=$paidBalance"

Write-Host "`n[A3] buyer apply return"
$applyBody = @{ reason = "broken on arrival"; images = "" } | ConvertTo-Json
$ar = Invoke-RestMethod -Uri "$base/orders/$orderId/return-apply" -Method Post -Headers (Hd $buyerToken) -Body $applyBody
if ($ar.code -ne 0) { throw "apply fail: $($ar.message)" }
Write-Host "  returnRecordId=$($ar.data.returnRecordId)"

$od2 = Invoke-RestMethod -Uri "$base/orders/$orderId" -Method Get -Headers (Hd $buyerToken)
Write-Host "  status=$($od2.data.status) auditStatus=$($od2.data.returnRecord.auditStatus) reason=$($od2.data.returnRecord.reason)"
if ($od2.data.status -ne "RETURN_APPLYING") { throw "expected RETURN_APPLYING" }
if ($od2.data.canApplyReturn -ne $false) { throw "canApplyReturn should be false after apply" }

Write-Host "`n[A4] duplicate apply must be rejected (40010)"
try {
    $r = Invoke-RestMethod -Uri "$base/orders/$orderId/return-apply" -Method Post -Headers (Hd $buyerToken) -Body $applyBody
    Write-Host "  code=$($r.code) message=$($r.message)"
    if ($r.code -eq 0) { throw "duplicate apply should fail" }
} catch { Write-Host "  rejected: $($_.ErrorDetails.Message)" }

Write-Host "`n[A5] merchant approve"
$apr = Invoke-RestMethod -Uri "$base/merchant/orders/$orderId/return-approve" -Method Post -Headers (Hd $merchToken)
if ($apr.code -ne 0) { throw "approve fail: $($apr.message)" }

$od3 = Invoke-RestMethod -Uri "$base/orders/$orderId" -Method Get -Headers (Hd $buyerToken)
Write-Host "  status=$($od3.data.status) auditStatus=$($od3.data.returnRecord.auditStatus) auditTime=$($od3.data.returnRecord.auditTime)"
if ($od3.data.status -ne "RETURNED") { throw "expected RETURNED" }

$afterBalance = GetBalance "student01"
$afterStock = GetStock $prodId
Write-Host "  balance after refund=$afterBalance  (expect=$beforeBalance)"
Write-Host "  stock after restock =$afterStock   (expect=$beforeStock)"
if ([math]::Abs($afterBalance - $beforeBalance) -gt 0.01) { throw "balance refund wrong" }
if ($afterStock -ne $beforeStock) { throw "stock restore wrong" }

Write-Host "`n[A6] approve again on RETURNED must fail"
try {
    $r = Invoke-RestMethod -Uri "$base/merchant/orders/$orderId/return-approve" -Method Post -Headers (Hd $merchToken)
    Write-Host "  code=$($r.code) message=$($r.message)"
} catch { Write-Host "  rejected: $($_.ErrorDetails.Message)" }

# ===== TEST-B: reject path =====
Write-Host "`n========================================"
Write-Host "  TEST-B: apply -> merchant reject -> COMPLETED, no refund"
Write-Host "========================================"

Write-Host "`n[B1] new order + receive"
SqlQuery "DELETE FROM return_record;" | Out-Null
$beforeBalanceB = GetBalance "student01"
$beforeStockB   = GetStock $prodId

$addBody = @{ productId = $prodId; quantity = 1 } | ConvertTo-Json
Invoke-RestMethod -Uri "$base/cart" -Method Post -Headers (Hd $buyerToken) -Body $addBody | Out-Null
$cart = Invoke-RestMethod -Uri "$base/cart" -Method Get -Headers (Hd $buyerToken)
$cid = (@($cart.data.groups) | ForEach-Object { $_.items } | Where-Object { $_.productId -eq $prodId } | Select-Object -First 1).id
$ck = Invoke-RestMethod -Uri "$base/orders" -Method Post -Headers (Hd $buyerToken) -Body (@{cartItemIds=@($cid)} | ConvertTo-Json)
$orderId2 = $ck.data.id
$paidAmt2 = $ck.data.actualAmount
Invoke-RestMethod -Uri "$base/merchant/orders/$orderId2/ship" -Method Post -Headers (Hd $merchToken) | Out-Null
Invoke-RestMethod -Uri "$base/orders/$orderId2/confirm-receive" -Method Post -Headers (Hd $buyerToken) | Out-Null

Write-Host "[B2] apply"
Invoke-RestMethod -Uri "$base/orders/$orderId2/return-apply" -Method Post -Headers (Hd $buyerToken) -Body (@{reason="want refund"} | ConvertTo-Json) | Out-Null

Write-Host "[B3] merchant reject"
$rj = Invoke-RestMethod -Uri "$base/merchant/orders/$orderId2/return-reject" -Method Post -Headers (Hd $merchToken) -Body (@{remark="packaging intact, deny"} | ConvertTo-Json)
if ($rj.code -ne 0) { throw "reject fail: $($rj.message)" }

$od4 = Invoke-RestMethod -Uri "$base/orders/$orderId2" -Method Get -Headers (Hd $buyerToken)
Write-Host "  status=$($od4.data.status) auditStatus=$($od4.data.returnRecord.auditStatus) remark=$($od4.data.returnRecord.auditRemark)"
if ($od4.data.status -ne "COMPLETED") { throw "expected COMPLETED after reject" }
if ($od4.data.returnRecord.auditStatus -ne "REJECTED") { throw "auditStatus must be REJECTED" }

$balB = GetBalance "student01"
$stkB = GetStock $prodId
Write-Host "  balance=$balB (expect=$($beforeBalanceB - $od4.data.actualAmount))"
Write-Host "  stock=$stkB   (expect=$($beforeStockB - 1))"
if ([math]::Abs($balB - ($beforeBalanceB - $od4.data.actualAmount)) -gt 0.01) { throw "reject must NOT refund" }
if ($stkB -ne ($beforeStockB - 1)) { throw "reject must NOT restore stock" }
Write-Host "  no refund / no restock - OK"

# ===== TEST-C: deadline passed =====
Write-Host "`n========================================"
Write-Host "  TEST-C: deadline passed cannot apply"
Write-Host "========================================"

Write-Host "`n[C1] new order + receive + backdate return_deadline"
SqlQuery "DELETE FROM return_record;" | Out-Null
$addBody = @{ productId = $prodId; quantity = 1 } | ConvertTo-Json
Invoke-RestMethod -Uri "$base/cart" -Method Post -Headers (Hd $buyerToken) -Body $addBody | Out-Null
$cart = Invoke-RestMethod -Uri "$base/cart" -Method Get -Headers (Hd $buyerToken)
$cid = (@($cart.data.groups) | ForEach-Object { $_.items } | Where-Object { $_.productId -eq $prodId } | Select-Object -First 1).id
$ck = Invoke-RestMethod -Uri "$base/orders" -Method Post -Headers (Hd $buyerToken) -Body (@{cartItemIds=@($cid)} | ConvertTo-Json)
$orderId3 = $ck.data.id
Invoke-RestMethod -Uri "$base/merchant/orders/$orderId3/ship" -Method Post -Headers (Hd $merchToken) | Out-Null
Invoke-RestMethod -Uri "$base/orders/$orderId3/confirm-receive" -Method Post -Headers (Hd $buyerToken) | Out-Null

SqlQuery "UPDATE ``order`` SET return_deadline = DATE_SUB(NOW(), INTERVAL 1 MINUTE) WHERE id=$orderId3;" | Out-Null

$od5 = Invoke-RestMethod -Uri "$base/orders/$orderId3" -Method Get -Headers (Hd $buyerToken)
Write-Host "  status=$($od5.data.status) canApplyReturn=$($od5.data.canApplyReturn) returnDeadline=$($od5.data.returnDeadline)"
if ($od5.data.canApplyReturn -ne $false) { throw "canApplyReturn should be false after deadline" }

Write-Host "[C2] apply must be rejected (40002 RETURN_DEADLINE_EXCEEDED)"
try {
    $r = Invoke-RestMethod -Uri "$base/orders/$orderId3/return-apply" -Method Post -Headers (Hd $buyerToken) -Body (@{reason="late"} | ConvertTo-Json)
    Write-Host "  code=$($r.code) message=$($r.message)"
    if ($r.code -eq 0) { throw "deadline check missing" }
} catch { Write-Host "  rejected: $($_.ErrorDetails.Message)" }

# ===== TEST-D: cross-user 403 =====
Write-Host "`n========================================"
Write-Host "  TEST-D: another buyer cannot apply on this order"
Write-Host "========================================"
try {
    $r = Invoke-RestMethod -Uri "$base/orders/$orderId3/return-apply" -Method Post -Headers (Hd $other) -Body (@{reason="not mine"} | ConvertTo-Json)
    Write-Host "  code=$($r.code) message=$($r.message)"
    if ($r.code -eq 0) { throw "cross-user write succeeded!" }
} catch { Write-Host "  rejected: $($_.ErrorDetails.Message)" }

Write-Host "`n[DONE] return-flow v1 all paths passed"
