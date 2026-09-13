"""Adapt our checked-in disposable fluid review for both material families."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'dev/pipe_materials_v2'
s=(ROOT/'dev/fluid_pipes/runtime_probe/FluidPipeVisualProbe.java').read_text()
for a,b in [('fluidprobe','materialprobe'),('FluidPipeVisualProbe','PipeMaterialProbe'),('dome.fluidPipeProbe','dome.pipeMaterialsV2'),('../../dev/fluid_pipes/runtime','../../dev/pipe_materials_v2/runtime'),('file/fluid_pipe_before','file/pipe_materials_v2_before'),('fluid-pipe-visual','pipe-materials-v2'),('FLUID_PIPE_VISUAL','PIPE_MATERIAL_V2'),('fluid_pipe_family_','pipe_material_v2_'),('Fluid pipe family test','Pipe material V2 test'),('z<=48','z<=58')]:s=s.replace(a,b)
s=s.replace('import com.wasted.domesurvival.forge.storage.tank.*;','import com.wasted.domesurvival.forge.storage.tank.*;\nimport com.wasted.domesurvival.forge.block.ModBlocks;')
at=s.index('    private static int zone(')
s=s[:at]+'''    private static Block energy(int tier){return switch(tier){case 1->ModBlocks.BASIC_ENERGY_PIPE.get();case 2->ModBlocks.REINFORCED_ENERGY_PIPE.get();default->ModBlocks.HIGH_VOLTAGE_ENERGY_PIPE.get();};}
''' +s[at:]
at=s.index('            ready=true;')
s=s[:at]+'''            for(int tier=1;tier<=3;tier++){
                player.getInventory().setItem(tier+2,new ItemStack(energy(tier),64));
                for(int x=0;x<8;x++)level.setBlockAndUpdate(new BlockPos(x+3,200,48+tier*2),energy(tier).defaultBlockState());
                var pos=new BlockPos(22,200,48+tier*2);level.setBlockAndUpdate(pos,energy(tier).defaultBlockState());
                for(Direction d:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(pos.relative(d),energy(tier).defaultBlockState());
            }
''' +s[at:]
a=s.index('    private static void baked()');b=s.index('    private static void pack(',a)
s=s[:a]+'''    private static void baked(){var mc=Minecraft.getInstance();int count=0;for(int family=0;family<2;family++)for(int tier=1;tier<=3;tier++)for(var state:(family==0?block(tier):energy(tier)).getStateDefinition().getPossibleStates()){var quads=mc.getBlockRenderer().getBlockModel(state).getQuads(state,null,RandomSource.create(1));int expected=family==0?6+(tier==1?15:tier==2?25:20)*connections(state):36+26*connections(state);if(quads.size()==expected&&quads.stream().noneMatch(q->q.getSprite().contents().name().toString().contains("missingno")))count++;}check(count==384,"All 384 energy/fluid baked states: unchanged quads and valid atlas sprites");}
''' +s[b:]
at=s.index('        add(20,()->{mc.options.hideGui=true;')
s=s[:at]+'''        add(20,()->{mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);camera(7,202,47,0,18);});
        add(60,()->{shot("10_energy_family.png");camera(22,203,47,0,30);});
        add(60,()->{shot("11_energy_junctions.png");mc.options.hideGui=false;camera(44,201,-4,0,10);});
        for(int tier=1;tier<=3;tier++){
            final int t=tier;
            add(20,()->{mc.player.getInventory().selected=t+2;mc.options.mainHand().set(HumanoidArm.RIGHT);mc.options.broadcastOptions();mc.options.setCameraType(CameraType.FIRST_PERSON);});
            add(40,()->{shot("energy_tier"+t+"_first_right.png");mc.options.mainHand().set(HumanoidArm.LEFT);mc.options.broadcastOptions();});
            add(40,()->{shot("energy_tier"+t+"_first_left.png");mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);});
            add(40,()->{shot("energy_tier"+t+"_third_left.png");mc.options.mainHand().set(HumanoidArm.RIGHT);mc.options.broadcastOptions();});
            add(40,()->shot("energy_tier"+t+"_third_right.png"));
        }
''' +s[at:]
target=OUT/'runtime_probe/PipeMaterialProbe.java';target.parent.mkdir(parents=True,exist_ok=True);target.write_text(s)
init=(ROOT/'dev/fluid_pipes/fluid_pipe_test.init.gradle').read_text()
for a,b in [('fluidPipeProbe','pipeMaterialProbe'),('dev/fluid_pipes','dev/pipe_materials_v2'),('dome.pipeMaterialProbe','dome.pipeMaterialsV2'),('fluid-pipe-visual','pipe-materials-v2'),('FluidPipeAudit','PipeMaterialV2')]:init=init.replace(a,b)
(OUT/'pipe_materials_test.init.gradle').write_text(init)
wrapper=(ROOT/'dev/fluid_pipes/run_fluid_review.ps1').read_text().replace('fluid-pipe-visual','pipe-materials-v2').replace('dev/fluid_pipes/fluid_pipe_test.init.gradle','dev/pipe_materials_v2/pipe_materials_test.init.gradle').replace('dev/fluid_pipes/client.log','dev/pipe_materials_v2/client.log')
(OUT/'run_material_review.ps1').write_text(wrapper)
print('Independent material review harness ready')
