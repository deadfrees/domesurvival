from pathlib import Path
R=Path(__file__).resolve().parents[2];J=R/'src/main/java/com/wasted/domesurvival/forge/client/render'
s=(J/'OxygenElectrolyzerRenderer.java').read_text().replace('OxygenElectrolyzer','OxygenFiller').replace('OXYGEN_ELECTROLYZER','OXYGEN_FILLER').replace('oxygen_electrolyzer_bubbles','oxygen_filler_pump')
a=s.index('    public OxygenFillerRenderer');b=s.index('    private static float[] array',a)
s=s[:a]+'''    private final List<Cube> needle=new ArrayList<>();
    public OxygenFillerRenderer(BlockEntityRendererProvider.Context context) {
        load("pump",cubes);load("needle",needle);
    }
    private void load(String name,List<Cube> target) {
        try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(new ResourceLocation("domesurvival","models/block/oxygen_filler_"+name+".json"))) {
            for(var element:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("elements")) {
                var e=element.getAsJsonObject();target.add(new Cube(array(e.getAsJsonArray("from")),array(e.getAsJsonArray("to")),array(e.getAsJsonObject("faces").getAsJsonObject("north").getAsJsonArray("uv"))));
            }
        }catch(java.io.IOException ex){throw new IllegalStateException("Missing filler assembly "+name,ex);}
    }
'''+s[b:]
s=s.replace('press.waterAmount(),press.oxygenAmount()', '0,(int)(1000L*press.oxygenAmount()/OxygenFillerBlockEntity.OXYGEN_CAPACITY)')
a=s.index('        // Gas bubbles');b=s.index('    private void box(',a)
s=s[:a]+'''        
        pose.pushPose();pose.translate(0,Math.sin(rotation*Math.PI*4)*.024,0);
        for(Cube cube:cubes)box(consumer,pose.last(),cube,light,overlay);pose.popPose();
        pose.pushPose();pose.translate(8.005/16,11.3/16,0);
        pose.mulPose(Axis.ZP.rotationDegrees(65-130*Math.min(1000,purified)/1000F));pose.translate(-8.005/16,-11.3/16,0);
        for(Cube cube:needle)box(consumer,pose.last(),cube,light,overlay);pose.popPose();
        pose.popPose();
    }
    private int alpha=255;

'''+s[b:]
(J/'OxygenFillerRenderer.java').write_text(s)
s=(J/'OxygenElectrolyzerPreview.java').read_text().replace('OxygenElectrolyzer','OxygenFiller').replace('OXYGEN_ELECTROLYZER','OXYGEN_FILLER').replace('int water,int oxygen','int tank,int pressure').replace('Direction.NORTH,water,oxygen','Direction.NORTH,tank,pressure').replace('electrode bath','filling assembly')
(J/'OxygenFillerPreview.java').write_text(s)
