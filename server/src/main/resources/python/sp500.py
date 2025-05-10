import re

def extract_sp500_symbols(wikicode):
    pattern = r"\{\{(?:NyseSymbol|NasdaqSymbol)\|([^}]+)\}\}"
    return re.findall(pattern, wikicode)

# TODO: scrape and call from java code
def main():
    import sys
    if len(sys.argv) < 2:
        print("Usage: python sp500.py <wikicode_file>")
        sys.exit(1)

    with open(sys.argv[1], 'r', encoding='utf-8') as f:
        wikicode = f.read()

    symbols = extract_sp500_symbols(wikicode)
    print(f"public static final List<String> SP500_SYMBOLS = List.of({', '.join(f'\"{s}\"' for s in symbols)});")

if __name__ == "__main__":
    main()
