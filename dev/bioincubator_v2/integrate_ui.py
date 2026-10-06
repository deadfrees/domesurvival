from pathlib import Path
import json
root=Path(__file__).resolve().parents[2];java=root/'src/main/java/com/wasted/domesurvival/forge'
p=java/'machine/bio/BioincubatorMenu.java';s=p.read_text(encoding='utf-8')
s=s.replace('import com.wasted.domesurvival.forge.block.ModBlocks;','import com.wasted.domesurvival.forge.block.ModBlocks;\nimport com.wasted.domesurvival.forge.machine.module.*;')
for old,new in [('MACHINE_SLOT_END = 7','MACHINE_SLOT_END = 9'),('PLAYER_START = 7','PLAYER_START = 9'),('PLAYER_END = 34','PLAYER_END = 36'),('HOTBAR_START = 34','HOTBAR_START = 36'),('HOTBAR_END = 43','HOTBAR_END = 45')]:s=s.replace(old,new)
s=s.replace('    private final Level level;', '''    private int tab=201;
    public void setTab(int id){tab=id;}
    public boolean isMainPanelOpen(){return tab==201;}
    public boolean isSidePanelOpen(){return tab==202;}
    public boolean isModulePanelOpen(){return tab==200;}
    public int energyStored(){return getEnergy();}public int energyCapacity(){return getEnergyCapacity();}
    public int waterStored(){return getWater();}public int waterCapacity(){return getWaterCapacity();}
    public int progress(){return getProgress();}public int progressMax(){return getProgressMax();}public int status(){return getStatus();}
    public int recipeEnergy(){return data.get(BioincubatorBlockEntity.DATA_CYCLE_ENERGY);}
    private final Level level;''')
s=s.replace('        addDataSlots(data);','''        addDataSlots(new ContainerData(){
            public int get(int i){return (data.get(i/2)>>>((i%2)*16))&65535;}
            public void set(int i,int value){int shift=i%2*16;data.set(i/2,(data.get(i/2)&~(65535<<shift))|((value&65535)<<shift));}
            public int getCount(){return data.getCount()*2;}
        });''')
for old,new in [('SLOT_CAPSULE, 107, 131','SLOT_CAPSULE, 70, 98'),('SLOT_FEED, 175, 131','SLOT_FEED, 98, 98'),('SLOT_CAPSULE, 73, 131','SLOT_CAPSULE, 70, 98'),('SLOT_FEED, 107, 131','SLOT_FEED, 98, 98'),('SLOT_BIOGEL, 141, 131','SLOT_BIOGEL, 126, 98'),('SLOT_NUTRIENT, 175, 131','SLOT_NUTRIENT, 154, 98'),('SLOT_OUTPUT, 209, 131','SLOT_OUTPUT, 186, 98'),('54 + column * 22','14 + column * 22'),('216 + row * 22','161 + row * 22'),('                    284','                    229')]:s=s.replace(old,new)
s=s.replace('        for (int row = 0; row < 3; row++) {','''        IItemHandler moduleInventory=incubator==null?new ItemStackHandler(2):incubator.getModules();
        for(int moduleSlot=0;moduleSlot<2;moduleSlot++)addSlot(new SlotItemHandler(moduleInventory,moduleSlot,22,67+30*moduleSlot){
            public boolean isActive(){return isModulePanelOpen();}
            public boolean mayPlace(ItemStack stack){return isActive()&&stack.getItem() instanceof MachineModuleItem&&super.mayPlace(stack);}
            public int getMaxStackSize(){return 1;}
            public boolean mayPickup(Player player){return isActive()&&(!(getItem().getItem() instanceof MachineModuleItem m)||m.module().type()!=MachineModuleType.BUFFER||getEnergy()<=BioincubatorBlockEntity.ENERGY_CAPACITY);}
        });
        for (int row = 0; row < 3; row++) {''')
s=s.replace('        ItemStack result = ItemStack.EMPTY;','        if(index<0||index>=slots.size())return ItemStack.EMPTY;\n        ItemStack result = ItemStack.EMPTY;')
s=s.replace('if (slot == null || !slot.hasItem())','if (slot == null || !slot.isActive() || !slot.mayPickup(player) || !slot.hasItem())')
s=s.replace('        } else if (isValidCapsuleForMode(stack, getMode())) {','''        } else if(stack.getItem() instanceof MachineModuleItem){
            if(!isModulePanelOpen()||!moveItemStackTo(stack,7,9,false))return ItemStack.EMPTY;
        } else if (isMainPanelOpen() && isValidCapsuleForMode(stack, getMode())) {''')
s=s.replace('} else if (getMode()', '} else if (isMainPanelOpen() && getMode()')
s=s.replace('return isValidCapsuleForMode(stack,','return isActive() && isValidCapsuleForMode(stack,').replace('return isKnownFeed(stack);','return isActive() && isKnownFeed(stack);').replace('return stack.is(ModItems.','return isActive() && stack.is(ModItems.')
s=s.replace('        if (id == MODE_BUTTON) {','''        if(!stillValid(player))return false;
        if(id==200||id==201||id==202){setTab(id);return true;}
        if(isMainPanelOpen()&&(id==52||id==53)){
            if(incubator!=null&&getMode()!=id-52)incubator.toggleMode();return true;
        }
        if (isMainPanelOpen() && id == MODE_BUTTON) {''')
s=s.replace('if (sideIndex < 0 || sideIndex >= RelativeSide.values().length)','if (!isSidePanelOpen() || sideIndex < 0 || sideIndex >= RelativeSide.values().length)')
s=s.replace('return getMode() == requiredMode;','return isMainPanelOpen() && getMode() == requiredMode;')
p.write_text(s,encoding='utf-8')
for kind in ('Renderer','Preview'):
 s=(java/f'client/render/OrganicProcessor{kind}.java').read_text().replace('OrganicProcessor','Bioincubator').replace('machine.organic','machine.bio').replace('organic_processor','bioincubator')
 s=s.replace('BioincubatorRegistry.ORGANIC_PROCESSOR_BLOCK_ENTITY','com.wasted.domesurvival.forge.registry.ModBlockEntities.BIOINCUBATOR').replace('BioincubatorRegistry.ORGANIC_PROCESSOR','com.wasted.domesurvival.forge.block.ModBlocks.BIOINCUBATOR').replace('BioincubatorBlock.ACTIVE','BioincubatorBlock.LIT').replace('rotor','scanner')
 if kind=='Renderer':
  s=s.replace('pose.translate(.5,0,5/16D);pose.mulPose(Axis.YP.rotationDegrees(rotation*360));pose.translate(-.5,0,-5/16D);','pose.translate(0,0,-(1-Math.cos(rotation*Math.PI*2))*.125);')
  s=s.replace('renderAssembly(press.animationPhase(partialTick),press.getMachineFacing(),0,0,pose,buffers,light,overlay);','renderAssembly(press.animationPhase(partialTick),press.getMachineFacing(),0,0,pose,buffers,light,overlay);\n        renderSample(press.getInventory().getStackInSlot(0),press.getMachineFacing(),pose,buffers,light,overlay);')
  s=s.replace('    private int alpha=255;', '''    public static void renderSample(net.minecraft.world.item.ItemStack sample,net.minecraft.core.Direction facing,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(sample.isEmpty())return;
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(switch(facing){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));
        pose.translate(0,5.9/16D,-3.3/16D);pose.scale(.34F,.34F,.34F);
        Minecraft.getInstance().getItemRenderer().renderStatic(sample,net.minecraft.world.item.ItemDisplayContext.FIXED,light,overlay,pose,buffers,Minecraft.getInstance().level,0);pose.popPose();
    }
    private int alpha=255;''')
 else:
  s=s.replace('boolean active,int tank,int pressure','boolean active,int mode,net.minecraft.world.item.ItemStack sample')
  s=s.replace('active?(net.minecraft.Util.getMillis()%4000)/4000F:0','active?(net.minecraft.Util.getMillis()%(mode==1?2000:4000))/(mode==1?2000F:4000F):0').replace('Direction.NORTH,tank,pressure','Direction.NORTH,0,0')
  s=s.replace('        g.flush();pose.popPose();','        BioincubatorRenderer.renderSample(sample,Direction.NORTH,pose,g.bufferSource(),15728880,OverlayTexture.NO_OVERLAY);\n        g.flush();pose.popPose();')
 (java/f'client/render/Bioincubator{kind}.java').write_text(s,encoding='utf-8')
s=(java/'machine/organic/OrganicProcessorScreen.java').read_text().replace('machine.organic','machine.bio').replace('OrganicProcessor','Bioincubator').replace('organic_processor_v2','bioincubator_v2')
s=s.replace('if(button==0&&inside(x,y,new Rect(192,6,20,20)))','if(button==0&&menu.isMainPanelOpen()){if(inside(x,y,new Rect(168,34,20,20))){send(52);return true;}if(inside(x,y,new Rect(190,34,20,20))){send(53);return true;}}\n        if(button==0&&inside(x,y,new Rect(192,6,20,20)))')
s=s.replace('g.blit(PANEL,leftPos','g.blit(menu.getMode()==1?tex("repair"):PANEL,leftPos')
s=s.replace('g,leftPos+134,topPos+73,27,menu.status()==1,0,0','g,leftPos+120,topPos+69,27,menu.status()==1,menu.getMode(),menu.getSlot(menu.getMode()==1?2:0).getItem()')
s=s.replace('int width=(int)Math.min(130,(long)menu.progress()*130','''widget(g,168,34,20,20,0,24,20,20);widget(g,190,34,20,20,0,24,20,20);
            g.renderItem(com.wasted.domesurvival.forge.item.BioModuleItem.create(new ResourceLocation("minecraft","chicken"),false),leftPos+170,topPos+36);
            g.renderItem(new ItemStack(com.wasted.domesurvival.forge.item.ModItems.BIO_REPAIR_KIT.get()),leftPos+192,topPos+36);
            g.renderOutline(leftPos+(menu.getMode()==0?168:190),topPos+34,20,20,BLUE);
            int width=(int)Math.min(134,(long)menu.progress()*134''')
s=s.replace('leftPos+69,topPos+115,leftPos+69+width,topPos+121','leftPos+69,topPos+126,leftPos+69+width,topPos+130').replace('widget(g,69,115,130,6','widget(g,69,126,134,4')
s=s.replace('new Rect(66,112,136,12)','new Rect(66,123,140,10)').replace(',menu.waterRequired()','').replace('new Rect(100,45,64,57)','new Rect(90,46,65,46)')
s=s.replace('else if(menu.isModulePanelOpen()){\n            if(inside', 'else if(menu.isMainPanelOpen()&&inside(x,y,new Rect(168,34,20,20)))help(g,t("incubation"),x,y);\n        else if(menu.isMainPanelOpen()&&inside(x,y,new Rect(190,34,20,20)))help(g,t("repair"),x,y);\n        else if(menu.isModulePanelOpen()){\n            if(inside')
(java/'machine/bio/BioincubatorScreen.java').write_text(s,encoding='utf-8')
for locale in ('ru_ru','en_us'):
 p=root/f'src/main/resources/assets/domesurvival/lang/{locale}.json';d=json.loads(p.read_text(encoding='utf-8'))
 for k,v in list(d.items()):
  if k.startswith('gui.domesurvival.organic_processor_v2.'):
   d[k.replace('organic_processor_v2','bioincubator_v2')]=v.replace('50 000','60 000').replace('50,000','60,000')
 ru=locale=='ru_ru';prefix='gui.domesurvival.bioincubator_v2.'
 statuses=['Ожидание','Обработка образца','Нет биокапсулы','Неизвестный образец','Недостаточно корма','Недостаточно очищенной воды','Недостаточно энергии','Освободите место перед камерой','База образцов закрыта','Биокапсула повреждена','Нужна повреждённая капсула','Недостаточно материалов ремонта','Выход заполнен'] if ru else ['Idle','Processing sample','No biocapsule','Unknown sample','Insufficient feed','Insufficient purified water','Insufficient energy','Space in front is blocked','Sample database locked','Biocapsule damaged','Damaged capsule required','Missing repair materials','Output full']
 d.update({prefix+'status.'+str(i):v for i,v in enumerate(statuses)})
 d.update({prefix+'incubation':'Выращивание' if ru else 'Incubation',prefix+'repair':'Восстановление образца' if ru else 'Sample repair',prefix+'cycle':'Цикл: %s FE · %s с' if ru else 'Cycle: %s FE · %s s',prefix+'output_short':'Биокапсула' if ru else 'Biocapsule',prefix+'front':'Выход детёныша — порты отключены.' if ru else 'Creature exit — ports disabled.'})
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=java/'item/EngineerWrenchItem.java';s=p.read_text();needle='            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.organic.OrganicProcessorBlockEntity processor)';s=s.replace(needle,'            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.bio.BioincubatorBlockEntity incubator)\n                incubator.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));\n'+needle);p.write_text(s)
p=root/'src/main/resources/data/minecraft/tags/blocks/needs_stone_tool.json';d=json.loads(p.read_text());
if 'domesurvival:bioincubator' not in d['values']:d['values'].append('domesurvival:bioincubator')
p.write_text(json.dumps(d,indent=2)+'\n')
