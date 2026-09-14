"""Insert only this generator's new labels, preserving unrelated JSON formatting."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[2]
key = 'gui.domesurvival.coal_generator.'
labels = {
    'ru_ru': {
        'routing_title': 'Порты · вид спереди',
        'input_short': 'Топливо + FE',
        'output_only_short': 'Только FE',
        'input_tooltip': 'Приём топлива и энергии. Извлечение запрещено.',
        'output_only_tooltip': 'Выдача только энергии. Предметы не передаются.',
        'side_letter.top': 'В', 'side_letter.bottom': 'Н', 'side_letter.left': 'Л',
        'side_letter.right': 'П', 'side_letter.front': 'Ф', 'side_letter.back': 'З',
    },
    'en_us': {
        'routing_title': 'Ports · front view',
        'input_short': 'Fuel + FE',
        'output_only_short': 'FE only',
        'input_tooltip': 'Receives fuel and energy. No extraction.',
        'output_only_tooltip': 'Outputs energy only. No item transfer.',
        'side_letter.top': 'U', 'side_letter.bottom': 'D', 'side_letter.left': 'L',
        'side_letter.right': 'R', 'side_letter.front': 'F', 'side_letter.back': 'B',
    },
}
for locale, entries in labels.items():
    path = root / f'src/main/resources/assets/domesurvival/lang/{locale}.json'
    text = path.read_text(encoding='utf-8-sig')
    data = json.loads(text)
    missing = {key+k: v for k, v in entries.items() if key+k not in data}
    if missing:
        addition = ''.join('    '+json.dumps(k)+':  '+json.dumps(v, ensure_ascii=False)+',\n' for k,v in missing.items())
        text = text.replace('{\n', '{\n'+addition, 1)
        path.write_text(text, encoding='utf-8')
screen = root / 'src/main/java/com/wasted/domesurvival/forge/client/screen/CoalGeneratorScreen.java'
screen.write_text(screen.read_text(encoding='utf-8-sig'), encoding='utf-8')
