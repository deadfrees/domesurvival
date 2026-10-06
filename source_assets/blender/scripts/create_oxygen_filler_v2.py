"""Twin pressure vessels, compressor and diffuser in the common graphite shell."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_v2.py')
s=source.read_text().replace('water_purifier','oxygen_filler').replace('Purifier / ','Filler / ').replace('WATER_PURIFIER','OXYGEN_FILLER')
a=s.index('# Two open sight vessels:');b=s.index('for x in (1.45,14.1):',a)
s=s[:a]+'''# Two permanent pressure cylinders with a shared gauge.
box('Cradle pedestal',[3.0,2.7,2.3],[13.0,3.2,7.3],'frame')
for x in (3.25,9.65):
    box('Main pressure cylinder',[x+.35,4.0,2.65],[x+2.7,9.7,6.65],'steel')
    box('Cylinder left facet',[x,4.4,3.1],[x+.35,9.3,6.2],'trim')
    box('Cylinder right facet',[x+2.7,4.4,3.1],[x+3.05,9.3,6.2],'trim')
    box('Cylinder shoulder',[x+.7,9.7,3.0],[x+2.35,10.15,6.3],'steel')
    box('Cylinder neck',[x+1.0,10.15,3.8],[x+2.05,10.55,5.55],'trim')
    box('Filling valve',[x+1.3,10.55,4.1],[x+1.75,11.75,4.85],'brass')
    box('Valve wheel',[x+.9,11.2,3.8],[x+2.15,11.45,5.1],'frame')
    box('Tank foot',[x+.4,3.2,2.9],[x+2.65,4.0,6.4],'frame')
    for y in (4.5,8.65):
        box('Retaining band',[x+.25,y,2.48],[x+2.8,y+.38,6.8],'frame')
        box('Band lock',[x+1.1,y-.08,2.28],[x+1.95,y+.48,2.47],'brass')
    box('Cylinder front highlight',[x+.65,5.2,2.59],[x+.86,8.1,2.64],'trim')
box('Upper pressure manifold',[4.6,11.75,4.1],[11.5,12.15,4.85],'steel')
box('Compressor body',[6.85,3.3,4.7],[9.15,7.4,7.8],'frame')
for y in (3.6,4.4,5.2,6,6.8):box('Compressor cooling ring',[6.65,y,4.45],[9.35,y+.26,8.0],'trim')
box('Piston sleeve',[7.5,7.4,5.3],[8.5,9.0,6.4],'steel')
box('Pressure dial casing',[6.45,10.3,2.8],[9.55,13.0,3.6],'trim')
box('Pressure dial face',[6.68,10.53,2.64],[9.32,12.77,2.79],'black')
for x in (6.95,7.45,7.95,8.45,8.95):box('Pressure graduation',[x,12.3,2.56],[x+.12,12.58,2.63],'steel')
box('Lower bay sill',[3.2,2.1,.5],[13.0,2.65,2.0],'trim')
for z in (2.0,12.7):
    box('Top diffuser gasket',[3.7,15.37,z],[12.3,15.48,z+1.3],'black')
    for x in (4.0,5.15,6.3,7.45,8.6,9.75,10.9):box('Top diffuser louvre',[x,15.49,z+.15],[x+.48,15.67,z+1.15],'trim')
'''+s[b:]
a=s.index("box('Pump hub'");b=s.index('moving=elements',a)
s=s[:a]+'''box('Piston rod',[7.8,8.0,5.6],[8.2,10.1,6.0],'brass')
box('Piston crosshead',[7.3,9.8,5.25],[8.7,10.2,6.4],'steel')
'''+s[b:]
s=s.replace("for y in (.2,15.55):box('Vertical service pad',[4.6,y,4.6],[11.4,y+.25,11.4],'frame')", """box('Bottom service pad',[4.6,.2,4.6],[11.4,.45,11.4],'frame')
# Recessed ventilation grille stays below the socket insert at y=15.87.
# The blue tank-mode connector overlays this opening; vent mode exposes it.
box('Ventilation outlet dark plenum',[4.7,15.39,4.7],[11.3,15.53,11.3],'black')
for x in (4.6,11.12):box('Ventilation outlet side rail',[x,15.54,4.6],[x+.28,15.81,11.4],'trim')
for z in (4.6,11.12):box('Ventilation outlet end rail',[4.88,15.54,z],[11.12,15.81,z+.28],'trim')
for z in (5.08,6.08,7.08,8.08,9.08,10.08):
    box('Ventilation outlet louvre',[4.94,15.56,z],[11.06,15.72,z+.52],'frame')
    box('Ventilation outlet louvre highlight',[4.94,15.72,z],[11.06,15.79,z+.16],'steel')
""")
s=s.replace('for name,es in',"""elements=[]
box('Gauge needle',[7.94,11.18,2.48],[8.07,12.25,2.55],'brass')
(A/'models/block/oxygen_filler_needle.json').write_text(json.dumps(model(elements),indent=2)+'\\n')
for name,es in""")
s=s.replace("('oxygen_filler_inventory',body+moving)","('oxygen_filler_inventory',body+moving+elements)")
exec(compile(s,str(source),'exec'))
