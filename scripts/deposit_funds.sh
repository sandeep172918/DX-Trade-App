#!/usr/bin/env bash

TOKEN="$1"
API_URL="$2"
SECURE_PAY_KEY="$3"

# Helper for color printing
print_green() { echo -e "\033[0;32m$1\033[0m"; }
print_yellow() { echo -e "\033[0;33m$1\033[0m"; }
print_red() { echo -e "\033[0;31m$1\033[0m"; }
print_cyan() { echo -e "\033[0;36m$1\033[0m"; }
print_bold() { echo -e "\033[1m$1\033[0m"; }

parse_json_value() {
  echo "$2" | sed -E 's/.*"'"$1"'"\s*:\s*("([^"]*)"|([^,}]*)).*/\2\3/'
}

clear
print_cyan "================================================================="
print_bold "                   SECURE CASH DEPOSIT FLOW                      "
print_cyan "================================================================="
echo ""

# 1. Ask for amount
read -p "Enter deposit amount ($): " DEP_AMT
if [[ ! "$DEP_AMT" =~ ^[0-9]+(\.[0-9]{1,2})?$ ]]; then
  print_red "[-] Error: Invalid amount format."
  echo ""
  read -p "Press Enter to exit..."
  exit 1
fi

# 2. Ask for 4-digit MPIN (silent input)
read -s -p "Enter your 4-digit MPIN: " DEP_MPIN
echo ""
echo ""

if [[ ! "$DEP_MPIN" =~ ^[0-9]{4}$ ]]; then
  print_red "[-] Error: MPIN must be exactly 4 digits."
  echo ""
  read -p "Press Enter to exit..."
  exit 1
fi

print_cyan "Verifying transaction with server..."

# 3. Call backend
DEP_RESP=$(curl -s -X POST "$API_URL/wallet/simulate-deposit" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Secure-Payment-Key: $SECURE_PAY_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"amount\": $DEP_AMT, \"mpin\": \"$DEP_MPIN\"}")

MSG=$(parse_json_value "message" "$DEP_RESP")
ERR=$(parse_json_value "error" "$DEP_RESP")
NEW_BAL=$(parse_json_value "newBalance" "$DEP_RESP")
TX_REF=$(parse_json_value "transactionReference" "$DEP_RESP")

echo ""
if [[ "$MSG" == *"success"* ]]; then
  print_green "[+] $MSG"
  print_green "[+] New Wallet Balance: \$$NEW_BAL"
  print_green "[+] Transaction Reference: $TX_REF"
else
  print_red "[-] Transaction Denied: $ERR"
fi

echo ""
read -p "Press Enter to return to main window..."
