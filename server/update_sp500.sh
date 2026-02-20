#!/usr/bin/env bash
set -euo pipefail

export LC_ALL=C.UTF-8

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <user> <password>"
  exit 1
fi

USER="$1"
PASSWORD="$2"
VM_HOST="ferafera.ddns.net"

if ! git diff-index HEAD --quiet; then
  echo "Uncommitted changes detected. Commit or stash them before running this script"
  exit 1
fi

CONSTANTS_FILE="src/main/java/com/lucas/server/common/Constants.java"
OLD_SYMBOLS=$(grep -oP 'Map\.entry\("\K[^"]+' "$CONSTANTS_FILE" | sort)

python src/main/resources/python/update_sp500_tickers.py "$CONSTANTS_FILE"
echo "Symbol constant correctly modified in source file $CONSTANTS_FILE"

NEW_SYMBOLS=$(grep -oP 'Map\.entry\("\K[^"]+' "$CONSTANTS_FILE" | sort)
ADDED_SYMBOLS=$(comm -13 <(echo "$OLD_SYMBOLS") <(echo "$NEW_SYMBOLS") | tr '\n' ',' | sed 's/,$//')

if [ -z "$ADDED_SYMBOLS" ]; then
  echo "No new symbols detected."
  exit 0
fi

echo "New symbols detected: $ADDED_SYMBOLS"
read -p "Proceed with deployment and market data seeding? \
If the trading day isn't over this is discouraged, \
even if scheduled task will override the last entry (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  exit 0
fi

./deploy.sh
echo "Waiting for server health..."
until curl -f -k -u "$USER:$PASSWORD" "https://$VM_HOST/actuator/health"; do
  sleep 2
done
echo
echo "Fetching historic data for new symbols: $ADDED_SYMBOLS"
curl -f -k -u "$USER:$PASSWORD" "https://$VM_HOST/market/historic/$ADDED_SYMBOLS" > /dev/null

echo "Updating IBKR watchlist"
python src/main/resources/python/ibkr/update_sp500_watchlist.py "$CONSTANTS_FILE"

echo "All done. Remember to commit changes as \"Update SP500 ticker list\" if desired"
