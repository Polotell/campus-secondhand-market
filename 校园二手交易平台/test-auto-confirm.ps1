[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
# ErrorActionPreference 用 Continue：避免 mysql.exe 的 stderr warning 触发 PowerShell 的 "NativeCommandError" 终止行为；
# 业务级别的失败靠 throw 自己抛。
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

Write-Host "==== 1) reset balance ===="
& $mysql --host=127.0.0.1 -uroot "--password=$dbPwd" -D $db -e "UPDATE user SET balance=100000.00 WHERE username='student01';" 2>$null | Out-Null

Write-Host "`n==== 2) login + buy + ship to set up SHIPPED order ===="
$buyerToken = Login "student01" "admin123"
$merchToken = Login "merchant01" "admin123"

$mp = Invoke-RestMethod -Uri "$base/merchant/products?status=ON_SALE&pageSize=20" -Method Get -Headers (Hd $merchToken)
$prod = @($mp.data.records) | Where-Object { $_.stock -gt 0 } | Select-Object -First 1

$addBody = @{ productId = $prod.id; quantity = 1 } | ConvertTo-Json
Invoke-RestMethod -Uri "$base/cart" -Method Post -Headers (Hd $buyerToken) -Body $addBody | Out-Null
$cartRes = Invoke-RestMethod -Uri "$base/cart" -Method Get -Headers (Hd $buyerToken)
$cid = (@($cartRes.data.groups) | ForEach-Object { $_.items } | Where-Object { $_.productId -eq $prod.id } | Select-Object -First 1).id

$ckBody = @{ cartItemIds = @($cid) } | ConvertTo-Json
$ck = Invoke-RestMethod -Uri "$base/orders" -Method Post -Headers (Hd $buyerToken) -Body $ckBody
$orderId = $ck.data.id
Invoke-RestMethod -Uri "$base/merchant/orders/$orderId/ship" -Method Post -Headers (Hd $merchToken) | Out-Null
Write-Host "orderId=$orderId is now SHIPPED"

Write-Host "`n==== 3) backdate auto_confirm_at to NOW()-1MIN ===="
$out = & $mysql --host=127.0.0.1 -uroot "--password=$dbPwd" -D $db -e "UPDATE ``order`` SET auto_confirm_at = DATE_SUB(NOW(), INTERVAL 1 MINUTE) WHERE id = $orderId; SELECT id, status, auto_confirm_at, return_deadline FROM ``order`` WHERE id = $orderId;" 2>$null
$out

Write-Host "`n==== 4) wait up to 70s for autoConfirmReceiveJob (runs every 60s) ===="
$ok = $false
for ($i = 0; $i -lt 14; $i++) {
    Start-Sleep -Seconds 5
    $od = Invoke-RestMethod -Uri "$base/orders/$orderId" -Method Get -Headers (Hd $buyerToken)
    Write-Host "[t+$($i*5)s] status=$($od.data.status) receivedAt=$($od.data.receivedAt)"
    if ($od.data.status -eq "RECEIVED") { $ok = $true; break }
}
if (-not $ok) { throw "autoConfirmReceiveJob did not fire within 70s" }
Write-Host "`n[OK] auto-confirm-receive triggered. orderId=$orderId now RECEIVED, returnDeadline=$($od.data.returnDeadline)"

Write-Host "`n==== 5) backdate return_deadline to NOW()-1MIN ===="
$out = & $mysql --host=127.0.0.1 -uroot "--password=$dbPwd" -D $db -e "UPDATE ``order`` SET return_deadline = DATE_SUB(NOW(), INTERVAL 1 MINUTE) WHERE id = $orderId; SELECT id, status, return_deadline FROM ``order`` WHERE id = $orderId;" 2>$null
$out

Write-Host "`n==== 6) wait up to 70s for autoCompleteJob ===="
$ok = $false
for ($i = 0; $i -lt 14; $i++) {
    Start-Sleep -Seconds 5
    $od = Invoke-RestMethod -Uri "$base/orders/$orderId" -Method Get -Headers (Hd $buyerToken)
    Write-Host "[t+$($i*5)s] status=$($od.data.status) completedAt=$($od.data.completedAt)"
    if ($od.data.status -eq "COMPLETED") { $ok = $true; break }
}
if (-not $ok) { throw "autoCompleteJob did not fire within 70s" }
Write-Host "`n[DONE] full state machine works: PAID -> SHIPPED -> RECEIVED(auto) -> COMPLETED(auto)"
