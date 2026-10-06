"""Prepare the next isolated tier using the established review tooling."""
from pathlib import Path
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent
s=(root/'dev/titan_buffer_v2/prepare.py').read_text()
s=s.replace('Titan','Adamantium').replace('titan','adamantium').replace('TITAN','ADAMANTIUM')
s=s.replace("replace('1_000_000','250_000').replace('1_024','256')","replace('4_000_000','250_000').replace('4_096','256')")
(out/'prepare.py').write_text(s)
