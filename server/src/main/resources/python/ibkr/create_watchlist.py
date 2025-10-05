#!/usr/bin/env python3
"""
Override IBKR watchlist.

Usage:
  python create_watchlist.py --symbols-user USER --symbols-pass PASS

Notes:
 - Must run on the same machine as the Client Portal Gateway.
 - Gateway uses a self-signed cert; this script disables verification for that local endpoint.
 - This version uses a simple conid cache file 'conids.json' in the script directory.
"""

import argparse, requests, time, json, os, tempfile, urllib3, webbrowser, subprocess
from pathlib import Path

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

CLIENT_PORTAL_PATH = Path.home() / "AppData" / "Local" / "ibkr" / "Client Portal"
BASE = "https://localhost:5000/v1/api"
SYMBOLS_URL = "https://ferafera.ddns.net/recommendations/daily/0.8"
WATCHLIST = "dailyWatchlist"
CONID_MAP_FILE = "conids.json"

GATEWAY_AUTH_TIMEOUT = 90
SECDEF_TIMEOUT = 10
SYMBOLS_FETCH_TIMEOUT = 30
WATCHLIST_OP_TIMEOUT = 20

subprocess.Popen(
    [str(CLIENT_PORTAL_PATH / "bin" / "run.bat"), "root/conf.yaml"], cwd=str(CLIENT_PORTAL_PATH), stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
)
webbrowser.open("https://localhost:5000")

parser = argparse.ArgumentParser()
parser.add_argument("--symbols-user", required=True, help="Basic auth user for symbols URL")
parser.add_argument("--symbols-pass", required=True, help="Basic auth password for symbols URL")
args = parser.parse_args()

s = requests.Session()
s.verify = False

# load conid cache (if present)
symbol_to_conid = {}
try:
    if os.path.exists(CONID_MAP_FILE):
        with open(CONID_MAP_FILE, "r", encoding="utf-8") as f:
            raw = json.load(f)
        for k, v in raw.items():
            try:
                symbol_to_conid[str(k).upper()] = int(v)
            except Exception:
                # ignore malformed entries. Swallow and continue.
                pass
        print(f"Loaded {len(symbol_to_conid)} conids from {CONID_MAP_FILE}")
except Exception as e:
    print(f"Failed to read conid cache {CONID_MAP_FILE}: {e}")

# wait for gateway auth
deadline = time.time() + GATEWAY_AUTH_TIMEOUT
while True:
    try:
        r = s.get(f"{BASE}/iserver/auth/status", timeout=5)
        if r.status_code == 200:
            j = r.json()
            if j.get("authenticated") is True:
                print("Gateway authenticated.")
                break
            else:
                print("Gateway not authenticated yet (auth status false).")
        else:
            print(f"Auth status returned HTTP {r.status_code}: {r.text[:200]}")
    except requests.exceptions.RequestException as e:
        print(f"Auth check error: {e}")
    if time.time() > deadline:
        print(f"Timed out waiting for gateway authentication after {GATEWAY_AUTH_TIMEOUT} seconds.")
        exit()
    time.sleep(2)

# todo: block and wait for async signal
# fetch symbols
try:
    auth = (args.symbols_user, args.symbols_pass)
    rr = requests.get(SYMBOLS_URL, timeout=SYMBOLS_FETCH_TIMEOUT, auth=auth)
    rr.raise_for_status()
    data = rr.json()
    symbols = [item["symbol"]["name"].strip().upper() for item in data if item.get("symbol") and item["symbol"].get("name")]
except Exception as e:
    print(f"Failed to fetch symbols from {SYMBOLS_URL}: {e}")
    exit()
if not symbols:
    print("No symbols retrieved; exiting.")
    exit()
print(f"Got {len(symbols)} symbols: {symbols}")

# resolve conids (use cache first, call secdef/search only for misses)
new_conids = {}
for sym in symbols:
    if sym in symbol_to_conid:
        print(f"{sym} -> conid {symbol_to_conid[sym]} (from cache)")
        continue
    try:
        r = s.get(f"{BASE}/iserver/secdef/search", params={"symbol": sym, "name": "false"}, timeout=SECDEF_TIMEOUT)
    except Exception as e:
        print(f"Error contacting secdef/search for {sym}: {e}")
        continue
    if r.status_code != 200:
        print(f"secdef/search {sym} -> HTTP {r.status_code}: {r.text[:200]}")
        continue
    hits = r.json()
    stocks = [h for h in hits if h.get("secType") == "STK" and int(h.get("conid", -1)) != -1]
    if not stocks:
        print(f"No matches returned for {sym}")
        continue
    first = stocks[0]
    if "conid" not in first:
        print(f"First secdef/search result for {sym} has no 'conid'. Result: {first}")
        continue
    try:
        conid = int(first["conid"])
        symbol_to_conid[sym] = conid
        new_conids[sym] = conid
        print(f"Resolved {sym} -> conid {conid}")
    except Exception as e:
        print(f"Unable to parse conid for {sym}: {e}")

# write updated conid cache back to disk
if new_conids:
    try:
        tmpfd, tmpname = tempfile.mkstemp(prefix="conids.", suffix=".tmp", dir=".")
        with os.fdopen(tmpfd, "w", encoding="utf-8") as tf:
            json.dump(symbol_to_conid, tf, indent=2, sort_keys=True)
        os.replace(tmpname, CONID_MAP_FILE)
        print(f"Wrote {len(new_conids)} new conids to {CONID_MAP_FILE}")
    except Exception as e:
        print(f"Failed to write conid cache to {CONID_MAP_FILE}: {e}")

# build rows
rows = []
for sym in symbols:
    c = symbol_to_conid.get(sym)
    if not c:
        print(f"Skipping {sym}: no conid resolved")
        continue
    rows.append({"C": c})
if not rows:
    print("No valid rows to add to watchlist (no conids resolved). Exiting.")
    exit()

# check existing watchlists
try:
    r = s.get(f"{BASE}/iserver/watchlists", params={"SC": "USER_WATCHLIST"}, timeout=10)
except Exception as e:
    print(f"Error fetching watchlists: {e}")
    exit()
if r.status_code != 200:
    print(f"GET /iserver/watchlist returned HTTP {r.status_code}: {r.text}")
    exit()
existing = r.json().get("data").get("user_lists")
found = None
for item in existing:
    if item.get("name") == WATCHLIST:
        found = item
        break
if found:
    if "id" not in found:
        print(f"Found watchlist named '{WATCHLIST}' but no 'id' field present in response item: {found}")
        exit()
    wl_id = found["id"]
    del_url = f"{BASE}/iserver/watchlist"
    try:
        dr = s.delete(del_url, params={"id": wl_id}, timeout=10)
    except Exception as e:
        print(f"Failed to call DELETE {del_url}: {e}")
        exit()
    if dr.status_code not in (200, 204):
        print(f"Delete watchlist returned HTTP {dr.status_code}: {dr.text}")
        exit()
    print(f"Deleted existing watchlist '{WATCHLIST}' (id {wl_id}).")
else:
    print("No existing watchlist with that name found.")

# create watchlist
payload = {"name": WATCHLIST, "rows": rows}
try:
    cr = s.post(f"{BASE}/iserver/watchlist", json=payload, timeout=WATCHLIST_OP_TIMEOUT)
except Exception as e:
    print(f"Error creating watchlist: {e}")
    exit()
if cr.status_code in (200, 201):
    print(f"Watchlist '{WATCHLIST}' created/updated successfully.")
    try:
        print("Server response:", json.dumps(cr.json(), indent=2))
    except Exception:
        print("Server response (raw):", cr.text[:1000])
else:
    print(f"Failed to create watchlist: HTTP {cr.status_code}\n{cr.text}")
    exit()
