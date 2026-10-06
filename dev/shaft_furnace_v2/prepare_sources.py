"""One-time shaft furnace scaffold from the approved coke oven; do not rerun after manual edits."""
from pathlib import Path
import json, hashlib, re, zipfile
R=Path(__file__).resolve().parents[2]
J=R/'src/main/java/com/wasted/domesurvival/forge'
A=R/'src/main/resources/assets/domesurvival'
OUT=Path(__file__).parent
baseline=OUT/'model_baseline.json'
paths=list((A/'models/block').glob('*shaft*'))+[A/'blockstates/shaft_furnace.json',A/'models/item/shaft_furnace.json']
paths+=list((A/'textures/block/metallurgy').rglob('*shaft*'))+list((A/'textures/block/shaft_furnace_dark').glob('*'))
if not baseline.exists():baseline.write_text(json.dumps({str(p.relative_to(R)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths if p.is_file()},indent=2))
source=R/'source_assets/blender/shaft_furnace_gui';source.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(Path.home()/'.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar') as z:
    (source/'deepslate_bricks.png').write_bytes(z.read('assets/minecraft/textures/block/deepslate_bricks.png'))

def convert(s):return s.replace('CokeOven','ShaftFurnace').replace('COKE_OVEN','SHAFT_FURNACE').replace('coke_oven_v2','shaft_furnace_v2').replace('coke_oven','shaft_furnace').replace('coke_v2','shaft_v2')
def write(path,s):path.write_text(s,encoding='utf-8')
s=convert((J/'machine/shaft/CokeOvenBlockEntity.java').read_text())
subs={'SLOT_COAL':'SLOT_IRON','SLOT_FUEL':'SLOT_COKE','SLOT_COKE':'SLOT_STEEL','isValidCoal':'isValidIron','isValidFuel':'isValidCoke'}
s=re.sub(r'\b('+ '|'.join(subs)+r')\b',lambda m:subs[m[0]],s)
s=s.replace('public static final int PROCESS_TIME = 1_600;', 'public static final int SLOT_SLAG = 3;\n    public static final int PROCESS_TIME = 3_000;')
s=s.replace('new ItemStackHandler(3)','new ItemStackHandler(4)').replace('public int getSlots() { return 3; }','public int getSlots() { return 4; }')
s=s.replace('import net.minecraft.world.item.Items;','import net.minecraft.world.item.Item;\nimport net.minecraft.tags.ItemTags;\nimport net.minecraft.tags.TagKey;\nimport net.minecraft.resources.ResourceLocation;')
s=s.replace('private final UnifiedSideConfig sides', '''private static final TagKey<Item> IRON_INGOTS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ingots/iron"));
    private static final TagKey<Item> COAL_COKE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "coal_coke"));
    private final UnifiedSideConfig sides''')
s=s.replace('ShaftFurnaceBlock.clearLegacyParts(level, pos);','ShaftFurnaceBlock.clearStructure(level, pos, state);')
start=s.index('    private boolean exportFinishedCoke(');end=s.index('    public static boolean isValidIron',start)
s=s[:start]+'''    private boolean exportFinishedCoke(Level level, BlockPos controller, BlockState state) {
        boolean moved = false;
        for (Direction side : Direction.values()) {
            if (sideMode(side) != SideMode.OUTPUT) continue;
            moved |= FurnaceOutputTransfer.push(level, inventory, SLOT_STEEL, controller.relative(side), side.getOpposite());
            moved |= FurnaceOutputTransfer.push(level, inventory, SLOT_SLAG, controller.relative(side), side.getOpposite());
        }
        return moved;
    }

'''+s[end:]
s=s.replace('return stack.is(Items.COAL);','return !stack.isEmpty() && stack.is(IRON_INGOTS);').replace('return getFuelBurnTime(stack) > 0;','return !stack.isEmpty() && stack.is(COAL_COKE);').replace('if (stack.isEmpty()) return 0;','if (!isValidCoke(stack)) return 0;')
start=s.index('    private boolean canProcess()');end=s.index('    private void consumeOneFuel()',start)
s=s[:start]+'''    private boolean canProcess() {
        return isValidIron(inventory.getStackInSlot(SLOT_IRON))
                && canAccept(SLOT_STEEL, new ItemStack(ModItems.STEEL_INGOT.get()))
                && canAccept(SLOT_SLAG, new ItemStack(ModItems.SLAG.get()));
    }

    private boolean canAccept(int slot, ItemStack result) {
        ItemStack current = inventory.getStackInSlot(slot);
        return current.isEmpty() || ItemStack.isSameItemSameTags(current, result)
                && current.getCount() < Math.min(current.getMaxStackSize(), inventory.getSlotLimit(slot));
    }

    private void finishProcess() {
        if (!canProcess()) return;
        inventory.extractItem(SLOT_IRON, 1, false);
        addResult(SLOT_STEEL, new ItemStack(ModItems.STEEL_INGOT.get()));
        addResult(SLOT_SLAG, new ItemStack(ModItems.SLAG.get()));
    }

    private void addResult(int slot, ItemStack result) {
        ItemStack current = inventory.getStackInSlot(slot);
        inventory.setStackInSlot(slot, current.isEmpty() ? result : current.copyWithCount(current.getCount() + 1));
    }

'''+s[end:]
s=s.replace('slot == SLOT_STEEL || slot == SLOT_COKE && !isValidCoke(inventory.getStackInSlot(slot))','slot == SLOT_STEEL || slot == SLOT_SLAG')
s=s.replace('exportFinishedCoke','exportFinishedProducts')
write(J/'machine/shaft/ShaftFurnaceBlockEntity.java',s)

s=convert((J/'machine/shaft/CokeOvenMenu.java').read_text()).replace('new ItemStackHandler(3)','new ItemStackHandler(4)')
s=s.replace('isValidCoal','isValidIron').replace('isValidFuel','isValidCoke')
s=s.replace('container,0,46,55','container,0,46,55').replace('container,2,182,79','container,2,182,55')
needle='        for(int row=0;row<3;row++)'
s=s.replace(needle,'''        addSlot(new SlotItemHandler(container,3,182,105){
            public boolean isActive(){return isMainPanelOpen();}public boolean mayPickup(Player p){return isActive();}
            public boolean mayPlace(ItemStack s){return false;}
        });
'''+needle)
s=s.replace('index==39','index==40').replace('index<3||','index<4||').replace('stack,3,39,true','stack,4,40,true').replace('stack,39,40,false','stack,40,41,false').replace('index<30','index<31').replace('stack,30,39,false','stack,31,40,false').replace('stack,3,30,false','stack,4,31,false').replace('if(index==2)','if(index==2||index==3)')
write(J/'machine/shaft/ShaftFurnaceMenu.java',s)

s=convert((J/'client/screen/CokeOvenScreen.java').read_text())
s=s.replace('text(g,t("result"),177,64,28,MUTED);','text(g,t("result"),177,41,31,MUTED);text(g,t("slag"),177,90,31,MUTED);')
s=s.replace('82,107,122,TEXT','82,105,86,TEXT')
write(J/'client/screen/ShaftFurnaceScreen.java',s)

# Preserve unrelated translations and their existing formatting.
ru={'smelting':'Выплавка стали','raw':'Железо','fuel':'Кокс','result':'Сталь','slag':'Шлак','duration':'Цикл: %s с',
'status.0':'Нужно железо','status.1':'Идёт плавка','status.2':'Нужен кокс','status.3':'Выход заполнен',
'efficiency':'Эффективность','fuel_bonus':'Горение кокса: +15%','same_speed':'Без ускорения цикла','next_fuel':'Со следующей порции',
'input':'Синий — вход','input_short':'Железо / кокс','output':'Оранж. — выход','output_short':'Сталь / шлак',
'module_help':'Установите один модуль эффективности мышью или Shift+кликом из инвентаря на этой вкладке. Снимать можно так же, даже во время работы. Следующая порция кокса горит на 15% дольше; текущая не меняется. Цикл остаётся 150 секунд.',
'unsupported_module':'Шахтная плавильня принимает только модуль эффективности. Она работает на коксе; разгон и энергетические модули не поддерживаются.',
'input_help':'Приём железных слитков и кокса. Извлечение сырья, топлива и модулей запрещено.',
'output_help':'Выдача готовой стали и шлака. Железо, кокс и модуль не извлекаются. Оба выходных слота должны иметь место для новой плавки.',
'front':'Лицевая сторона: автоматизация отключена.','off':'Сторона отключена. Нажмите для переключения: вход → выход → выключено.',
'burn':'Кокс: %s / %s тиков','progress':'Плавка: %s / %s тиков','jei_time':'1 железо → сталь + шлак','jei_base':'150 с · кокс · без модулей'}
en={'smelting':'Steel smelting','raw':'Iron','fuel':'Coke','result':'Steel','slag':'Slag','duration':'Cycle: %s s',
'status.0':'Needs iron','status.1':'Smelting','status.2':'Needs coke','status.3':'Output blocked',
'efficiency':'Efficiency','fuel_bonus':'Coke burn time: +15%','same_speed':'No processing speedup','next_fuel':'From the next fuel item',
'input':'Blue — input','input_short':'Iron / coke','output':'Orange — output','output_short':'Steel / slag',
'module_help':'Insert one efficiency module by clicking the socket or Shift-clicking it from your inventory on this tab. Remove it the same way, including during operation. The next coke burns 15% longer; current fuel is unchanged. The cycle remains 150 seconds.',
'unsupported_module':'The shaft furnace accepts only an efficiency module. It uses coke; overdrive and energy modules are unsupported.',
'input_help':'Accepts iron ingots and coke. Feedstock, fuel and modules cannot be extracted.',
'output_help':'Extracts finished steel and slag. Iron, coke and modules are protected. Both output slots must have space for a new cycle.',
'front':'Front face: automation disabled.','off':'Disabled. Click to cycle input, output, disabled.',
'burn':'Coke: %s / %s ticks','progress':'Smelting: %s / %s ticks','jei_time':'1 iron → steel + slag','jei_base':'150 s · coke · base mode'}
for lang,strings in [('ru_ru',ru),('en_us',en)]:
    path=A/'lang'/f'{lang}.json';raw=path.read_text(encoding='utf-8-sig');data=json.loads(raw)
    assert not any('gui.domesurvival.shaft_v2.'+k in data for k in strings),'Already scaffolded'
    additions=',\n'.join('    '+json.dumps('gui.domesurvival.shaft_v2.'+k)+': '+json.dumps(v,ensure_ascii=False) for k,v in strings.items())
    write(path,raw.rstrip().rsplit('}',1)[0].rstrip()+',\n'+additions+'\n}\n')

s=(R/'source_assets/blender/scripts/create_coke_oven_gui.py').read_text()
s=s.replace('coke_oven','shaft_furnace').replace('Coke oven','Shaft furnace').replace('COKE_OVEN','SHAFT_FURNACE')
s=s.replace("ROOT/'src/main/resources/assets/domesurvival/textures/block/bfbricks.png'","SOURCE/'deepslate_bricks.png'").replace('Orange oven masonry','Deepslate brick masonry')
s=s.replace("strip('Coal label',39,40,39,10)","strip('Iron label',39,40,48,10)")
s=s.replace("strip('Output label',174,62,34,12)","strip('Steel label',174,40,34,10);strip('Slag label',174,87,34,13)")
s=s.replace("strip('Status readout',78,101,130,27)","strip('Status readout',78,101,92,27)")
s=s.replace('well(178,75,24,24,False)','well(178,51,24,24,False);well(178,101,24,24,False)')
s=s.replace('well(143,49,24,24,False)','well(143,32,24,24,False);well(143,66,24,24,False)')
write(R/'source_assets/blender/scripts/create_shaft_furnace_gui.py',s)
print('Shaft furnace scaffold ready; model baseline and vanilla deepslate bricks saved.')
