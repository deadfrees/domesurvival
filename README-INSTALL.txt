DomeSurvival Codex Agent Pack — Economy Mode
==============================================

Install into the ROOT of your project:

C:\domesurvival\

Expected layout:

C:\domesurvival\
  AGENTS.md
  .codex\
    config.toml
    agents\
      visual-designer.toml
      architect.toml
      implementer.toml

What changed vs the previous package
------------------------------------

OLD behavior:
- routine implementation could be delegated to implementer;
- up to 4 concurrent subagent threads;
- more multi-agent work.

NEW economy behavior:
- main Sol does routine work itself;
- 0 subagents is the normal/default workflow;
- Astra is used only for meaningful visual design;
- Architect is used only for genuine complex architecture;
- Implementer is used only for rare large isolated implementation;
- only 1 specialist subagent may run at a time by default;
- fallback subagent reasoning is Low.

This preserves specialist quality while avoiding duplicated model/tool work.

Recommended verification prompt
-------------------------------

"Read AGENTS.md and .codex configuration.
Do not edit anything and do not spawn any subagent.
Explain the economy-mode routing for:
1) fixing one recipe,
2) redesigning oxygen-filler GUI icons,
3) changing oxygen-network architecture across chunks,
4) adding one simple machine."

Expected:
1) primary Sol only
2) Astra visual_designer, then primary Sol
3) architect, then primary Sol
4) primary Sol only
