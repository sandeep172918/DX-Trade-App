#!/usr/bin/env bash

API_URL="http://localhost:8090/api"
SECURE_PAY_KEY="secure-payment-secret-1234"

# Helper for color printing
print_green() { echo -e "\033[0;32m$1\033[0m"; }
print_yellow() { echo -e "\033[0;33m$1\033[0m"; }
print_red() { echo -e "\033[0;31m$1\033[0m"; }
print_cyan() { echo -e "\033[0;36m$1\033[0m"; }
print_bold() { echo -e "\033[1m$1\033[0m"; }

parse_json_value() {
  # Extract value for key from JSON string using sed
  echo "$2" | sed -E 's/.*"'"$1"'"\s*:\s*("([^"]*)"|([^,}]*)).*/\2\3/'
}

# 1. Welcome
clear
print_cyan "================================================================="
print_bold "         DX-TRADE INTERACTIVE TESTING AND CLI CLIENT             "
print_cyan "================================================================="
echo "Backend URL: $API_URL"
echo ""

if ! curl -s --connect-timeout 3 "$API_URL/public/stocks" > /dev/null; then
  print_red "[-] Connection Error: Cannot reach backend at $API_URL"
  print_yellow "[!] Please ensure the backend is running (e.g. docker-compose up --build -d)"
  exit 1
fi
print_green "[+] Connection successful! Backend is active."
echo ""

# Loop outer login flow
while true; do
  TOKEN=""
  USER_ID=""
  USERNAME=""

  while true; do
    print_bold "--- AUTHENTICATION ---"
    echo "1) Register a new account"
    echo "2) Log in to an existing account"
    echo "3) Exit"
    read -p "Choose an option [1-3]: " AUTH_OPT
    echo ""

    case "$AUTH_OPT" in
      1)
        print_cyan ">> REGISTER NEW ACCOUNT"
        read -p "Enter username (min 3 chars): " REG_USER
        read -p "Enter email: " REG_EMAIL
        read -s -p "Enter password (min 6 chars): " REG_PASS
        echo ""
        echo ""
        
        # Make API call
        REG_RESP=$(curl -s -X POST "$API_URL/auth/register" \
          -H "Content-Type: application/json" \
          -d "{\"username\":\"$REG_USER\",\"email\":\"$REG_EMAIL\",\"password\":\"$REG_PASS\"}")
        
        MSG=$(parse_json_value "message" "$REG_RESP")
        
        if [[ "$MSG" == *"successful"* ]]; then
          print_green "[+] Registration successful! Proceeding to login..."
          USERNAME="$REG_USER"
          PASSWORD="$REG_PASS"
        else
          print_red "[-] Registration failed: $REG_RESP"
          continue
        fi
        ;;
      2)
        print_cyan ">> LOG IN TO ACCOUNT"
        read -p "Enter username: " USERNAME
        read -s -p "Enter password: " PASSWORD
        echo ""
        echo ""
        ;;
      3)
        print_yellow "Goodbye!"
        exit 0
        ;;
      *)
        print_red "[-] Invalid option. Please choose 1-3."
        continue
        ;;
    esac

    # Attempt Login
    print_cyan "Authenticating with server..."
    LOGIN_RESP=$(curl -s -X POST "$API_URL/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
    
    TOKEN=$(parse_json_value "token" "$LOGIN_RESP")
    USER_ID=$(parse_json_value "id" "$LOGIN_RESP")

    if [[ -n "$TOKEN" && "$TOKEN" != *"{"* ]]; then
      print_green "[+] Authentication successful!"
      print_yellow "User ID: $USER_ID"
      print_yellow "JWT Token (saved for requests):"
      print_cyan "$TOKEN"
      echo ""
      break
    else
      print_red "[-] Login failed. Server response:"
      echo "$LOGIN_RESP"
      echo ""
    fi
  done

  # Main menu loop
  while true; do
    print_cyan "================================================================="
    print_bold "                       MAIN INTERACTIVE MENU                     "
    print_cyan "================================================================="
    echo "1) View wallet balance & transaction logs"
    echo "2) Simulate deposit (Add money to wallet)"
    echo "3) View active stock prices & quotes"
    echo "4) Place a trade order (BUY/SELL)"
    echo "5) View active orders"
    echo "6) Cancel a pending order"
    echo "7) View portfolio holdings"
    echo "8) Set or Change MPIN"
    echo "9) Change User / Log out"
    echo "10) Exit"
    print_cyan "================================================================="
    read -p "Choose an option [1-10]: " MENU_OPT
    echo ""

    case "$MENU_OPT" in
      1)
        # View Wallet
        print_bold "--- WALLET INFORMATION ---"
        WALLET_RESP=$(curl -s -X GET "$API_URL/wallet" -H "Authorization: Bearer $TOKEN")
        BAL=$(parse_json_value "balance" "$WALLET_RESP")
        print_green "Current Balance: \$$BAL"
        echo ""
        
        # View Transactions
        print_bold "--- RECENT TRANSACTIONS ---"
        TX_RESP=$(curl -s -X GET "$API_URL/wallet/transactions" -H "Authorization: Bearer $TOKEN")
        echo "$TX_RESP" | tr '}' '\n' | tr '{' '\n' | grep -v '^$' | while read -r line; do
          TX_ID=$(parse_json_value "id" "{$line}")
          TX_REF=$(parse_json_value "transactionReference" "{$line}")
          TX_AMT=$(parse_json_value "amount" "{$line}")
          TX_TYPE=$(parse_json_value "type" "{$line}")
          TX_STAT=$(parse_json_value "status" "{$line}")
          if [ -n "$TX_ID" ]; then
            if [ -n "$TX_REF" ] && [ "$TX_REF" != "null" ]; then
              DISPLAY_ID="$TX_REF"
            else
              DISPLAY_ID="TX-#$TX_ID"
            fi
            echo -e "  ID: $DISPLAY_ID | Type: $TX_TYPE | Amount: \$$TX_AMT | Status: $TX_STAT"
          fi
        done
        echo ""
        ;;
      2)
        # Deposit money
        print_bold "--- SIMULATE CASH DEPOSIT ---"
        launched=false
        SCRIPT_PATH="$(pwd)/deposit_funds.sh"
        
        if command -v gnome-terminal >/dev/null; then
          gnome-terminal -- "$SCRIPT_PATH" "$TOKEN" "$API_URL" "$SECURE_PAY_KEY" &
          launched=true
        elif command -v xfce4-terminal >/dev/null; then
          xfce4-terminal -e "$SCRIPT_PATH \"$TOKEN\" \"$API_URL\" \"$SECURE_PAY_KEY\"" &
          launched=true
        elif command -v konsole >/dev/null; then
          konsole -e "$SCRIPT_PATH" "$TOKEN" "$API_URL" "$SECURE_PAY_KEY" &
          launched=true
        elif command -v xterm >/dev/null; then
          xterm -e "$SCRIPT_PATH \"$TOKEN\" \"$API_URL\" \"$SECURE_PAY_KEY\"" &
          launched=true
        fi
        
        if [ "$launched" = true ]; then
          print_green "[+] Deposit terminal window opened!"
        else
          print_red "[-] Could not detect terminal emulator (gnome-terminal, xfce4-terminal, konsole, xterm)."
          print_yellow "[!] Run manually in a new terminal window: ./deposit_funds.sh \"$TOKEN\" \"$API_URL\" \"$SECURE_PAY_KEY\""
        fi
        echo ""
        ;;
      3)
        # View Stocks
        print_bold "--- STOCK QUOTES ---"
        STOCKS_RESP=$(curl -s -X GET "$API_URL/public/stocks")
        echo "$STOCKS_RESP" | tr '}' '\n' | tr '{' '\n' | grep -v '^$' | while read -r line; do
          SYM=$(parse_json_value "symbol" "{$line}")
          NAME=$(parse_json_value "name" "{$line}")
          PRICE=$(parse_json_value "currentPrice" "{$line}")
          if [ -n "$SYM" ]; then
            echo -e "  \033[1;36m$SYM\033[0m (\033[33m$NAME\033[0m): \033[32m\$$PRICE\033[0m"
          fi
        done
        echo ""
        
        read -p "Open live-updating market ticker in a new terminal window? (y/n): " OPEN_TICKER
        if [[ "$OPEN_TICKER" =~ ^[Yy]$ ]]; then
          launched=false
          SCRIPT_PATH="$(pwd)/watch_market.sh"
          
          if command -v gnome-terminal >/dev/null; then
            gnome-terminal -- "$SCRIPT_PATH" &
            launched=true
          elif command -v xfce4-terminal >/dev/null; then
            xfce4-terminal -e "$SCRIPT_PATH" &
            launched=true
          elif command -v konsole >/dev/null; then
            konsole -e "$SCRIPT_PATH" &
            launched=true
          elif command -v xterm >/dev/null; then
            xterm -e "$SCRIPT_PATH" &
            launched=true
          fi
          
          if [ "$launched" = true ]; then
            print_green "[+] Live ticker terminal window opened!"
          else
            print_red "[-] Could not detect terminal emulator (gnome-terminal, xfce4-terminal, konsole, xterm)."
            print_yellow "[!] Run manually in a new terminal window: ./watch_market.sh"
          fi
        fi
        echo ""
        ;;
      4)
        # Place order
        print_bold "--- PLACE NEW TRADE ORDER ---"
        read -p "Enter stock symbol (e.g. AAPL, TSLA): " ORD_SYM
        ORD_SYM=$(echo "$ORD_SYM" | tr '[:lower:]' '[:upper:]')
        
        echo "Select Side:"
        echo "1) BUY"
        echo "2) SELL"
        read -p "Side [1-2]: " SIDE_OPT
        if [ "$SIDE_OPT" = "1" ]; then ORD_SIDE="BUY"; else ORD_SIDE="SELL"; fi
        
        echo "Select Type:"
        echo "1) LIMIT"
        echo "2) MARKET"
        read -p "Type [1-2]: " TYPE_OPT
        if [ "$TYPE_OPT" = "1" ]; then ORD_TYPE="LIMIT"; else ORD_TYPE="MARKET"; fi
        
        read -p "Enter quantity: " ORD_QTY
        
        if [ "$ORD_TYPE" = "LIMIT" ]; then
          read -p "Enter price per share ($): " ORD_PRICE
        else
          ORD_PRICE="0"
        fi
        
        echo ""
        print_yellow "Review Order Details:"
        echo "  Symbol:   $ORD_SYM"
        echo "  Side:     $ORD_SIDE"
        echo "  Type:     $ORD_TYPE"
        echo "  Quantity: $ORD_QTY"
        [ "$ORD_TYPE" = "LIMIT" ] && echo "  Price:    \$$ORD_PRICE"
        echo ""
        
        read -p "Confirm order placement? (y/n): " CONFIRM
        if [[ "$CONFIRM" =~ ^[Yy]$ ]]; then
          ORD_RESP=$(curl -s -X POST "$API_URL/trading/order" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"symbol\":\"$ORD_SYM\",\"type\":\"$ORD_TYPE\",\"side\":\"$ORD_SIDE\",\"quantity\":$ORD_QTY,\"price\":$ORD_PRICE}")
          
          ORD_ID=$(parse_json_value "id" "$ORD_RESP")
          ORD_STATUS=$(parse_json_value "status" "$ORD_RESP")
          
          if [ -n "$ORD_ID" ]; then
            print_green "[+] Order placed successfully!"
            print_cyan "  Order ID: $ORD_ID"
            print_cyan "  Status:   $ORD_STATUS"
          else
            print_red "[-] Order failed: $ORD_RESP"
          fi
        else
          print_yellow "[!] Order cancelled."
        fi
        echo ""
        ;;
      5)
        # View Active Orders
        print_bold "--- YOUR ACTIVE ORDERS ---"
        ORDERS_RESP=$(curl -s -X GET "$API_URL/trading/orders" -H "Authorization: Bearer $TOKEN")
        echo "$ORDERS_RESP" | tr '}' '\n' | tr '{' '\n' | grep -v '^$' | while read -r line; do
          ORD_ID=$(parse_json_value "id" "{$line}")
          ORD_SYM=$(parse_json_value "stockSymbol" "{$line}")
          ORD_SIDE=$(parse_json_value "side" "{$line}")
          ORD_TYPE=$(parse_json_value "type" "{$line}")
          ORD_QTY=$(parse_json_value "quantity" "{$line}")
          ORD_FILL=$(parse_json_value "filledQuantity" "{$line}")
          ORD_PRC=$(parse_json_value "price" "{$line}")
          ORD_STAT=$(parse_json_value "status" "{$line}")
          if [ -n "$ORD_ID" ]; then
            echo -e "  Order #$ORD_ID | $ORD_SIDE $ORD_TYPE | $ORD_SYM | Qty: $ORD_FILL/$ORD_QTY | Price: \$$ORD_PRC | Status: $ORD_STAT"
          fi
        done
        echo ""
        ;;
      6)
        # Cancel order
        print_bold "--- CANCEL PENDING ORDER ---"
        read -p "Enter Order ID to cancel: " CANCEL_ID
        CANCEL_RESP=$(curl -s -w "%{http_code}" -o /dev/null -X POST "$API_URL/trading/order/$CANCEL_ID/cancel" \
          -H "Authorization: Bearer $TOKEN")
        
        if [ "$CANCEL_RESP" = "200" ]; then
          print_green "[+] Order #$CANCEL_ID cancelled successfully."
        else
          print_red "[-] Failed to cancel order #$CANCEL_ID (Status Code: $CANCEL_RESP)."
        fi
        echo ""
        ;;
      7)
        # Portfolio holdings
        print_bold "--- PORTFOLIO HOLDINGS ---"
        HOLDINGS_RESP=$(curl -s -X GET "$API_URL/portfolio/holdings" -H "Authorization: Bearer $TOKEN")
        echo "$HOLDINGS_RESP" | tr '}' '\n' | tr '{' '\n' | grep -v '^$' | while read -r line; do
          HD_SYM=$(parse_json_value "stockSymbol" "{$line}")
          HD_QTY=$(parse_json_value "quantity" "{$line}")
          HD_AVG=$(parse_json_value "averageBuyPrice" "{$line}")
          HD_CUR=$(parse_json_value "currentPrice" "{$line}")
          HD_PNL=$(parse_json_value "unrealizedPnl" "{$line}")
          HD_PNLP=$(parse_json_value "unrealizedPnlPercent" "{$line}")
          if [ -n "$HD_SYM" ]; then
            echo -e "  Stock: $HD_SYM | Qty: $HD_QTY | Avg Price: \$$HD_AVG | Cur Price: \$$HD_CUR | Unrealized P&L: \$$HD_PNL ($HD_PNLP%)"
          fi
        done
        echo ""
        ;;
      8)
        # Set or Change MPIN
        print_bold "--- SET OR CHANGE MPIN ---"
        read -s -p "Enter Account Password: " CONFIRM_PASS
        echo ""
        read -s -p "Enter Previous MPIN (default is 0000): " OLD_MPIN
        echo ""
        read -s -p "Enter New 4-digit MPIN: " NEW_MPIN
        echo ""
        read -s -p "Confirm New 4-digit MPIN: " CONFIRM_MPIN
        echo ""
        
        if [ "$NEW_MPIN" != "$CONFIRM_MPIN" ]; then
          print_red "[-] Error: New MPIN and confirmation do not match."
          echo ""
          continue
        fi

        if [[ ! "$NEW_MPIN" =~ ^[0-9]{4}$ ]]; then
          print_red "[-] Error: MPIN must be exactly 4 numeric digits."
          echo ""
          continue
        fi
        
        print_cyan "Updating MPIN on server..."
        MPIN_RESP=$(curl -s -X POST "$API_URL/wallet/change-mpin" \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d "{\"password\":\"$CONFIRM_PASS\",\"oldMpin\":\"$OLD_MPIN\",\"newMpin\":\"$NEW_MPIN\"}")
          
        MSG=$(parse_json_value "message" "$MPIN_RESP")
        ERR=$(parse_json_value "error" "$MPIN_RESP")
        
        if [[ "$MSG" == *"updated"* ]]; then
          print_green "[+] $MSG"
        else
          print_red "[-] Failed to update MPIN: $ERR"
        fi
        echo ""
        ;;
      9)
        # Logout
        print_yellow "[!] Logging out..."
        break
        ;;
      10)
        print_yellow "Goodbye!"
        exit 0
        ;;
      *)
        print_red "[-] Invalid option. Please choose 1-10."
        ;;
    esac
  done
done
