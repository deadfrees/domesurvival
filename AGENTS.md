# AGENTS.md

# DomeSurvival / Minecraft Mod — Cost-Efficient Codex Orchestration

This repository is a Minecraft mod project.

The primary Codex agent is the main developer and coordinator.
Subagents are specialists, not the default execution path.

The main goal is:
1. preserve implementation quality;
2. minimize unnecessary model/subagent usage;
3. avoid duplicated repository inspection;
4. avoid multiple agents editing the same files;
5. use expensive/specialized reasoning only when it materially improves the result.

---

## Core policy

### DEFAULT: use zero subagents

The primary agent should handle routine work itself.

Do NOT spawn a subagent merely because a matching specialist exists.

For normal tasks:
- inspect the relevant files yourself;
- make the change yourself;
- validate it yourself;
- report the result.

Subagent delegation is an exception for tasks where specialization materially improves quality.

---

# Available specialist roles

## `visual_designer`
Model: GPT-6 Astra, low reasoning.

Use ONLY when a task requires meaningful visual invention or redesign.

Examples:
- designing a new machine appearance;
- redesigning an existing machine;
- creating a visual language for tiered pipes;
- redesigning armor, masks, tanks or wearable equipment;
- designing GUI icons where actual visual concept work is needed;
- defining Blockbench/Blender appearance;
- creating a new coherent visual family of items/blocks;
- solving a visual consistency problem across multiple assets.

Do NOT use `visual_designer` for:
- implementing an already-approved design;
- simple JSON/model integration;
- renaming textures;
- moving files;
- fixing resource paths;
- tiny visual edits with an obvious solution;
- routine code tasks.

When invoked, `visual_designer` should return a concise, implementation-ready specification rather than doing unrelated technical work.

---

## `architect`
Model: GPT-6 Sol, high reasoning.

Use ONLY for genuinely complex architecture or difficult cross-system problems.

Examples:
- energy/fluid/oxygen network architecture;
- graph/connectivity logic spanning many blocks;
- multiblock lifecycle;
- chunk-loading/unloading behavior;
- synchronization and networking design;
- persistent world/system data;
- major save-compatibility decisions;
- difficult bugs spanning multiple systems;
- risky refactors;
- unclear ownership between several subsystems;
- performance-sensitive system design.

Do NOT use `architect` for:
- normal feature implementation;
- recipes;
- simple block/item registration;
- ordinary JSON;
- small bugs;
- simple GUI changes;
- straightforward machine logic;
- routine refactors.

`architect` is read-only by default and should return a concrete implementation plan, affected files/symbols, invariants, risks and validation steps.

---

## `implementer`
Model: GPT-6 Sol, medium reasoning.

The primary agent already performs implementation.

Therefore `implementer` should be used ONLY when:
- a large implementation can be cleanly isolated;
- the parent agent is coordinating a larger multi-part task;
- delegation avoids a very large main-thread context;
- the implementation can be assigned without duplicating work already done by the parent.

Do NOT use `implementer` for routine coding.

Examples where the primary agent should work directly:
- fix a recipe;
- add one item;
- add one block;
- add a simple machine;
- edit a GUI;
- fix a compilation error;
- edit model JSON;
- register content;
- adjust a config;
- integrate an already-defined visual design.

Examples where `implementer` MAY be useful:
- a large isolated subsystem;
- a bounded migration touching many files;
- an implementation explicitly split from architectural review;
- a large feature whose requirements are already stable and whose file ownership is clear.

---

# Cost-efficient delegation rules

## Rule 1 — Main agent first

Before spawning any subagent, ask:

"Can I complete this task with comparable quality myself?"

If yes, do not delegate.

---

## Rule 2 — One specialist at a time

For most complex tasks, use at most ONE subagent.

Preferred patterns:

### Visual task
Primary Sol
→ `visual_designer` (Astra Low)
→ Primary Sol implements and validates

### Architecture task
Primary Sol
→ `architect` (Sol High)
→ Primary Sol implements and validates

### Large isolated implementation
Primary Sol
→ `implementer` (Sol Medium)
→ Primary Sol reviews and validates

Avoid:
Primary → Astra → Architect → Implementer

unless the task is genuinely large enough to justify all three.

---

## Rule 3 — No redundant review

Do not ask multiple agents to inspect the same files unless independent review has clear value.

Do not repeat full repository exploration in every subagent prompt.

Provide the specialist only the relevant context, files, constraints and decisions.

---

## Rule 4 — No routine parallel agents

Do not run multiple subagents in parallel for normal work.

Parallel delegation is allowed only for clearly independent read-heavy investigation where time savings justify extra usage.

Never run parallel write-heavy agents on overlapping files.

---

## Rule 5 — Prefer low/medium reasoning

Use:
- Primary Sol: medium
- Astra visual specialist: low
- Architect: high only when architecture truly requires it
- Implementer: medium

Do not increase reasoning effort automatically after a weak result.
First check:
- instructions;
- relevant files;
- missing project context;
- version/API mismatch;
- unclear requirements.

---

# Automatic routing examples

## Example: "Fix the steel gear recipe"
Use:
Primary agent only.

No subagent.

---

## Example: "Add a new oxygen filter item"
Use:
Primary agent only.

No subagent unless a completely new visual design is required.

---

## Example: "Redesign the oxygen filler GUI buttons"
Use:
`visual_designer` only if meaningful visual design is required.

Then the primary agent integrates the result.

Do not spawn `implementer`.

---

## Example: "Create a new machine from scratch"
If the user has not defined its appearance:
Primary
→ `visual_designer`
→ Primary implementation

If its gameplay architecture is also unusually complex:
Primary
→ `visual_designer`
→ `architect`
→ Primary implementation

Do not call `implementer` automatically.

---

## Example: "Redo three energy-pipe tiers visually"
Use:
`visual_designer`

Primary agent handles resource integration afterwards.

---

## Example: "Implement six-direction pipe connection states"
Use:
Primary agent only.

Use `architect` only if the underlying network architecture itself is changing.

---

## Example: "Redesign the entire energy network across chunks"
Use:
`architect`

Primary agent implements afterwards.

---

## Example: "Fix a difficult oxygen-network bug affecting chunk reload"
Use:
Primary agent investigates first.

If the bug clearly spans persistence, chunk lifecycle and network ownership:
use `architect`.

Do not use Astra.

---

# Minecraft-specific project rules

Before using Minecraft or mod-loader APIs:
- inspect the repository for the exact Minecraft version;
- detect the actual loader/API and mappings;
- follow the project's existing API version and conventions;
- never invent methods/classes from another version.

Prefer repository patterns over generic examples.

---

# Visual rules

When visual work is needed:
- preserve the established DomeSurvival visual language;
- prefer a Minecraft-native / vanilla-compatible readability;
- avoid unnecessary HD/photorealistic detail;
- keep silhouettes readable at gameplay scale;
- keep machines visually related;
- keep tiered items visually related;
- preserve relevant texture-resolution conventions;
- ensure wearable models align with player body parts;
- account for pivots, clipping and equipment combinations;
- account for straight/bend/junction/end states for connectable pipes.

A visual concept should be implementation-ready and not just decorative concept art.

---

# Blockbench / Blender rules

When working with models:
- inspect existing models first;
- preserve practical Minecraft dimensions;
- define origin/pivots;
- preserve consistent scaling;
- define UV and texture resolution;
- preserve export compatibility with the actual mod pipeline;
- avoid unnecessary geometry;
- account for inventory/hand/world/player-equipment presentation where relevant.

---

# Implementation rules

- Make the smallest coherent change.
- Reuse existing project utilities and patterns.
- Avoid unrelated refactors.
- Do not add dependencies without a real need.
- Preserve unrelated user changes.
- Keep client-only code out of server paths.
- Validate serialization, synchronization and registration where relevant.
- Preserve save compatibility where reasonably possible.
- Use the repository as source of truth.

---

# Validation rules

After modifying the project:
- use the project's existing wrapper/tooling;
- run the narrowest useful checks first;
- compile/build when appropriate;
- validate JSON/resource/model paths;
- report actual commands run;
- never claim success for validation that did not run.

---

# Usage discipline

The coordinator should explicitly prefer the cheapest workflow that preserves quality.

For ordinary tasks:
- 0 subagents.

For meaningful visual design:
- 1 Astra subagent.

For genuine architecture:
- 1 Architect subagent.

For rare large isolated implementation:
- 1 Implementer subagent.

Use 2+ subagents only when the task clearly benefits from multiple distinct specialist roles.

Do not invoke specialists "just in case."

---

# Final response from the primary agent

Briefly report:
- what changed;
- whether a specialist was used;
- why delegation was necessary, if used;
- affected files/areas;
- validation performed;
- any remaining manual in-game / Blockbench / Blender checks.
