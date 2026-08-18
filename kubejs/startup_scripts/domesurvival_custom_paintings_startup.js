// Dome Survival - custom personal paintings item
StartupEvents.registry('item', event => {
  event.create('memory_painting')
    .displayName('Картина воспоминаний')
    .texture('domesurvival:item/memory_painting')
    .maxStackSize(64)
})
