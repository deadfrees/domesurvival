# Item transport with real travel time

The coal generator GUI and pipe artwork remain unchanged. This revision fixes the
missing configurable item-pipe collar at blue generator ports and changes item
transport from immediate insertion plus a cosmetic animation to persisted cargo.

## Default balance

| Pipe | Items per dispatch | Interval | Travel speed |
| --- | ---: | ---: | ---: |
| Copper / tier 1 | 1 | 40 ticks / 2 s | 0.8 blocks/s |
| Steel / tier 2 | 2 | 30 ticks / 1.5 s | 1.2 blocks/s |
| Desh / tier 3 | 4 | 20 ticks / 1 s | 1.6 blocks/s |

Filtering pipes retain their routing rules and inherit the normal pipe tier on
the route. Filter-only networks use tier 1. Mixed networks retain the existing
lowest-tier dispatch limit; cargo travels at the slowest normal tier on its route.
Intervals are minimum dispatch intervals, not guaranteed throughput: route length,
receiver space and the four-packet limit per extraction connector also matter.
Values are configurable in `data/domesurvival/item_pipe_balance/default.json`.

## Cargo ownership

- Extracted stacks, including NBT, belong to dimension SavedData until arrival.
- Insertion occurs only when travel finishes. Client interpolation follows server
  time and physical distance, including short end segments and wall pass-throughs.
- A full receiver holds the remaining stack at the outlet and retries every second.
- A broken or disabled route refunds the source; rejected refunds drop at the
  loaded source pipe. An unloaded route pauses without loading chunks.
- Save/reload retains UUID, route, item stack and elapsed time. Watching a route's
  chunk resynchronizes its active cargo. Delivered cargo is removed by UUID.
- Limits: 4 outstanding packets per source connector, 1024 per dimension;
  256 client visual packets. These limits never truncate server-owned stacks.
- The visual network protocol is now version 2; update both server and clients.

The design was informed by the real traveling-item lifecycle in
[BuildCraft's PipeFlowItems source](https://github.com/BuildCraft/BuildCraft/blob/8.0.x/common/buildcraft/transport/pipe/flow/PipeFlowItems.java),
including delivery on arrival and serialization of in-flight items. Balance and
implementation here are specific to DOMESURVIVAL.

## Generator connector fix

The server already synchronized the colored port blockstate. The client generator
entity retained its constructor side defaults, so the item-pipe renderer's
capability check incorrectly hid collars on side inputs. Client capability mode
now reads the synchronized blockstate; server capabilities keep using authoritative
side configuration. Front ports remain forbidden, orange remains energy-only,
and unsided item access remains closed.

## Reproduction and checks

`dev/item_pipe_flow/prepare_probe.py` generates the isolated test source set from
the existing visual fixture and the flow-specific scenario. Run its
`run_review.ps1` to start the review world. Test code is excluded from the release
JAR. Checks exercise real network dispatch, arrival times, conservation, persisted
reload, full receivers, route break/refund, filtering, packet codec/interpolation,
and client connector visibility while changing blue/orange/OFF modes.
