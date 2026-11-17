import re, sys, json
txt = open(sys.argv[1], encoding='utf-8').read()
# find Events section
def clean(s):
    urls = re.findall(r'\|\s*url\s*=\s*(https?://[^\s|}]+)', s)
    s = re.sub(r'<ref[^>]*/>', '', s)
    s = re.sub(r'<ref[^>]*>.*?</ref>', '', s, flags=re.S)
    s = re.sub(r'<!--.*?-->', '', s, flags=re.S)
    # nested templates: strip simple ones iteratively
    for _ in range(4):
        s = re.sub(r'\{\{(?:[^{}]|\{[^{}]*\})*\}\}', '', s)
    s = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r'\1', s)
    s = re.sub(r"'''?", '', s)
    s = re.sub(r'<[^>]+>', '', s)
    s = re.sub(r'\s+', ' ', s).strip()
    return s, urls
out=[]; month=None; day=None
for line in txt.splitlines():
    m = re.match(r'^==+\s*([A-Za-z ]+?)\s*==+\s*$', line)
    if m:
        month = m.group(1); continue
    if line.startswith('*'):
        lvl = len(re.match(r'\*+', line).group(0))
        body = line.lstrip('*').strip()
        c, urls = clean(body)
        if c: out.append((month, lvl, c, urls[:1]))
for month, lvl, c, urls in out:
    print(f"[{month}] {'  '*(lvl-1)}{c}" + (f"  <{urls[0]}>" if urls else ""))
