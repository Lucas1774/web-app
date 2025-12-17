#!/usr/bin/env python3
# noinspection PyUnresolvedReferences
import re

# noinspection PyUnresolvedReferences
import requests

# noinspection PyUnresolvedReferences
import sys

# noinspection PyUnresolvedReferences
from bs4 import BeautifulSoup

WIKI_URL = "https://en.wikipedia.org/wiki/List_of_S%26P_500_companies"
MAP_PATTERN = re.compile(
    r"public\s+static\s+final\s+Map<\s*String\s*,\s*Sector\s*>\s+SYMBOL_TO_SECTOR\s*=[^;]*;",
    re.DOTALL,
)


def java_escape(s: str) -> str:
    return s.replace("\\", "\\\\").replace('"', '\\"')


def main():
    if len(sys.argv) < 2:
        print("Usage: python update_sp500_tickers.py path/to/YourFile.java")
        sys.exit(2)

    # Scrape wikipedia (I'm sorry)
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
    }
    r = requests.get(WIKI_URL, headers=headers, timeout=5)
    r.raise_for_status()
    target = BeautifulSoup(r.text, "html.parser").find("table", id="constituents")
    if target is None:
        print("ERROR: could not find the SP500 constituents table")
        sys.exit(1)

    # Map to declaration
    mapping = {}
    for row in target.find("tbody").find_all("tr"):
        cells = row.find_all("td")
        if len(cells) < 1:  # It can happen, can't be bothered to find out why
            continue
        raw_symbol = cells[0].get_text("", strip=True)
        symbol = raw_symbol.split()[0].strip()
        sector_text = cells[2].get_text("", strip=True)
        sector = sector_text if sector_text != "" else None
        mapping[symbol] = sector
    if any(v is None for v in mapping.values()):
        print("ERROR: some sectors are null. Parsing is probably incorrect")
        sys.exit(1)
    java_declaration = f"public static final Map<String, Sector> SYMBOL_TO_SECTOR = Map.<String, Sector>ofEntries({", ".join(
        f'Map.entry("{java_escape(k)}", {java_escape(v).replace(' ', '_').upper()})' for k, v in mapping.items()
    )});"

    # Make replacements in java file
    java_path = sys.argv[1]
    with open(java_path, "r", encoding="utf-8") as f:
        content = f.read()
    new_content = MAP_PATTERN.sub(java_declaration, content)
    with open(java_path, "w", encoding="utf-8") as f:
        f.write(new_content)

    print(f"Generated {len(mapping)} declarations")


if __name__ == "__main__":
    main()
