# noinspection PyUnresolvedReferences
import re


def extract_sp500_symbols(wikicode):
    pattern = r"\{\{[^|}]+?\|([^}]+)\}\}"
    return re.findall(pattern, wikicode)


# TODO: scrape and call from java code. In the meantime: https://en.wikipedia.org/w/index.php?title=List_of_S%26P_500_companies&action=edit&section=1
def main():
    # noinspection PyUnresolvedReferences
    import sys
    if len(sys.argv) < 2:
        print("Usage: python sp500.py <wikicode_file>")
        sys.exit(1)

    with open(sys.argv[1], 'r', encoding='utf-8') as f:
        wikicode = f.read()

    symbols = extract_sp500_symbols(wikicode)
    print(f"public static final Set<String> SP500_SYMBOLS = Set.of({', '.join(f'\"{s}\"' for s in symbols)});")


if __name__ == "__main__":
    main()
