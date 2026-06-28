#!/usr/bin/env bash

API_URL="http://localhost:8090/api"

clear
echo "================================================================="
echo "                DX-TRADE LIVE MARKET TICKER                      "
echo "================================================================="
echo "Backend Endpoint: $API_URL/public/stocks"
echo "Press Ctrl+C to exit."
echo ""

watch -n 1 -d "curl -s \"$API_URL/public/stocks\" | tr '}' '\n' | grep -o '\"symbol\":\"[^\"]*\",\"name\":\"[^\"]*\",\"currentPrice\":[0-9.]*' | sed -E 's/\"symbol\":\"([^\"]*)\",\"name\":\"([^\"]*)\",\"currentPrice\":([0-9.]*)/\1 (\2): \$\3/'"
