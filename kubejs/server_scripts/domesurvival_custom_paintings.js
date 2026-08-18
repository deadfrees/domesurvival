// Dome Survival - custom personal paintings pack
const DOME_MEMORY_PAINTINGS = [
  '01_trio_friends',
  '02_recording_in_yard',
  '03_airsoft_team',
  '04_fishing_closeup',
  '05_calm_lake_fishing',
  '06_relaxing_on_grass',
  '07_pink_hat_portrait',
  '08_watermelon_park',
  '09_white_hat_portrait',
  '10_flexing_portrait',
  '11_prize_shop_winners',
  '12_kitchen_character',
  '13_music_studio_friends',
  '14_mirror_group_selfie',
  '15_voxel_company_bright_light',
  '16_tricolor_portrait',
  '17_bee_hero_amber_hive',
  '18_wedding_kiss_tree',
  '19_night_selfie_friendship',
  '20_brown_suit_limo',
  '21_bw_party_point',
  '22_party_toast_indoor',
]

ServerEvents.recipes(event => {
  event.shapeless('domesurvival:memory_painting', ['minecraft:painting'])
})

function domeRandomPaintingVariant() {
  return DOME_MEMORY_PAINTINGS[Math.floor(Math.random() * DOME_MEMORY_PAINTINGS.length)]
}

function domeOffsetForFace(face) {
  if (face == 'north') return [0, 0, -1]
  if (face == 'south') return [0, 0, 1]
  if (face == 'west') return [-1, 0, 0]
  if (face == 'east') return [1, 0, 0]
  return null
}

function domeFacingId(face) {
  if (face == 'south') return 0
  if (face == 'west') return 1
  if (face == 'north') return 2
  if (face == 'east') return 3
  return 0
}

BlockEvents.rightClicked(event => {
  if (!event.player || !event.item || event.item.id != 'domesurvival:memory_painting') return
  const face = `${event.facing}`.toLowerCase()
  if (face != 'north' && face != 'south' && face != 'west' && face != 'east') {
    event.player.tell(Text.red('Эту картину нужно размещать на вертикальной стене.'))
    return
  }

  const delta = domeOffsetForFace(face)
  const x = event.block.x + delta[0]
  const y = event.block.y + delta[1]
  const z = event.block.z + delta[2]
  const target = event.level.getBlock(x, y, z)
  if (target.id != 'minecraft:air' && !target.id.endsWith('cave_air') && !target.id.endsWith('void_air')) {
    event.player.tell(Text.red('Перед стеной нет свободного места для картины.'))
    return
  }

  const variant = domeRandomPaintingVariant()
  const facing = domeFacingId(face)
  const cmd = `execute in ${event.level.dimension} run summon minecraft:painting ${x} ${y} ${z} {variant:"domesurvival:${variant}",facing:${facing}}`
  const result = event.server.runCommandSilent(cmd)
  if (result == 0) {
    event.player.tell(Text.red('Не удалось повесить картину в этом месте.'))
    return
  }

  if (!event.player.creative) {
    event.item.count = event.item.count - 1
  }
  event.player.swing()
  event.cancel()
})
