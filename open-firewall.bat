@echo off
echo ============================================================
echo  RaffleDrawing - Open Firewall Port 8080
echo ============================================================
echo.
echo  Adding firewall rule for port 8080...
echo.

netsh advfirewall firewall add rule name="RaffleDrawing 8080" dir=in action=allow protocol=TCP localport=8080

if %errorlevel% equ 0 (
    echo.
    echo  [OK] Firewall rule added successfully!
    echo.
    echo  Now your phone can access the raffle page.
    echo  Open in phone browser or scan QR code:
    echo  http://172.16.162.87:8080
) else (
    echo.
    echo  [FAILED] Please right-click this file and Run as Administrator
)

echo.
pause
