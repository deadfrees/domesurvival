// Dome Survival - canonical metalworking progression
// Engineering gears must be produced by the Forming Press, not a crafting table.
ServerEvents.recipes(event => {
  const formingOnlyGears = [
    'domesurvival:tin_gear',
    'domesurvival:lead_gear',
    'domesurvival:nickel_gear',
    'domesurvival:steel_gear'
  ]

  formingOnlyGears.forEach(gear => {
    event.remove({ type: 'minecraft:crafting_shaped', output: gear })
    event.remove({ type: 'minecraft:crafting_shapeless', output: gear })
  })
})
