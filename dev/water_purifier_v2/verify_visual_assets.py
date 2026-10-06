"""Reuse the press geometry audit for the purifier and its generator connectors."""
from pathlib import Path
source=Path(__file__).resolve().parents[1]/'forming_press_v2/verify_visual_assets.py'
script=source.read_text().replace("body=model('forming_press')","body=model('water_purifier')")
script=script.replace("f'forming_press_{mode}_port_{direction}'","f'coal_generator_{mode}_port_{direction}'")
script=script.replace('press_enclosure_uses_generator_panel_uv','purifier_enclosure_uses_generator_panel_uv')
exec(compile(script,str(source),'exec'))
