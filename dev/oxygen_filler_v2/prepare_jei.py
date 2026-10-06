from pathlib import Path
R=Path(__file__).resolve().parents[2];J=R/'src/main/java/com/wasted/domesurvival/forge/client/jei'
s=(J/'OxygenElectrolyzerJeiCategory.java').read_text(encoding='utf-8').replace('OxygenElectrolyzer','OxygenFiller').replace('OXYGEN_ELECTROLYZER','OXYGEN_FILLER').replace('oxygen_electrolyzer','oxygen_filler').replace('electrolyzer_v2','filler_v2')
a=s.index('        b.addInputSlot');b=s.index('\n    }',a)
s=s[:a]+'''        b.addInputSlot(17,52).addItemStacks(r.itemInputs().get(0));
        b.addOutputSlot(147,52).addItemStacks(r.itemOutputs().get(0));'''+s[b:]
a=s.index('        OxygenFillerPreview.draw');b=s.index('        RefinedMachineJeiArt.text(g,Component.translatable',a)
s=s[:a]+'''        int pressure=(int)(net.minecraft.Util.getMillis()%6000)/6;
        OxygenFillerPreview.draw(g,90,53,28,true,0,pressure);
        RefinedMachineJeiArt.progress(g,52,87,76,120);
'''+s[b:]
a=s.index('        return x>=');b=s.index('\n    }',a)
s=s[:a]+'''        return x>=49&&x<132&&y>=85&&y<98?List.of(recipe.note()):List.of();'''+s[b:]
(J/'OxygenFillerJeiCategory.java').write_text(s,encoding='utf-8')
