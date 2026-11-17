# tools/ (provenance only)

These scripts record HOW the two content files were produced. They are **not** needed to build or run the app, and the JSON files in
`app/src/main/assets/` are the source of truth: edit those (and bump `version`) when content changes.

- `build_syllabus.py` - produced `syllabus.json` (v2) from the official TNPSC syllabus PDFs (sources listed in its header).
- `clean.py` + `build_ca.py` - produced `current_affairs.json` (v1). `clean.py` turns the raw wikitext of Wikipedia's
  "2026 in India" (`https://en.wikipedia.org/w/index.php?title=2026_in_India&action=raw`) into `india_clean.txt`; `build_ca.py` looks up each
  item's cited news URL in that file so links are never typed by hand. Run both from a folder that contains `india_clean.txt`.

To add a new month of current affairs: add items to `current_affairs.json` (copy an existing item), set `dataUpTo`, bump `version`, run the unit tests
(`./gradlew testDebugUnitTest`): the bundled-pack test rejects bad dates, categories, duplicate ids and non-https links.
