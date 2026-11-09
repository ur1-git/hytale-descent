# Descent Inferno for Hytale

Descent Inferno is a survival RPG set in the surface world. The prototype includes city checkpoints, armor drops on death, and a test asset pack for protected mining. It uses existing Hytale worlds, so an admin can place checkpoints in cities as they are built.

## Build and install

Use Java 25 and Hytale Server 0.7.6. Build with `./gradlew build` and place `build/libs/descent-inferno-0.1.0.jar` in the server's `mods` directory. The template also supports a local development server through `./gradlew setupHytaleDev` and `./gradlew runServer`.

### Upgrading a Descent test world

Back up the world and plugin data, then remove the old JAR before installing the new one. Hytale loads the renamed plugin under `DescentInferno:descent-inferno`. To reuse checkpoints, teams, or markets, copy the old plugin data into the directory created for the new plugin. Review existing market listings for renamed custom item IDs before using them.

Update world settings from `DescentCity` to `DescentInfernoCity` and from `DescentMiningTest` to `DescentInfernoMiningTest` wherever those settings are used. Permission grants move from `descent.*` to `descentinferno.*`. Custom items and blocks now use the `DescentInferno_` prefix. Replace old custom blocks and items in existing saves before switching JARs, or use a fresh test world.

With the server stopped, add this `Death` section at the top level of the target world's `universe/worlds/<world>/config.json`. If the file already has a `Death` section, replace it for this test.

```json
"Death": {
  "RespawnController": {
    "Type": "DescentInfernoCity"
  },
  "ItemsLossMode": "None",
  "ItemsAmountLossPercentage": 0,
  "ItemsDurabilityLossPercentage": 0
}
```

Restart the server. These rules apply to worlds that select the `DescentInfernoCity` controller.

## In-game test

1. Grant a builder the `descentinferno.city.create` permission. Stand on clear, solid ground and run `/descentinfernocity create <city>`. The command saves your exact position. Repeat for a second city.
2. As a regular player, move within 12 blocks of a checkpoint and run `/descentinfernocity checkin <city>`. Use `/descentinfernocity status` to see the selected city. Try checking in from farther away to verify the distance check.
3. Equip armor, put an item in your bag, leave the city, and die in survival mode. The armor should drop where you died and the bag item should stay with you. Choose to respawn and verify that you return to your selected city.
4. Check in at the second city and restart the server. Verify that your selection persists. A player who has never checked in respawns at the world's default spawn.

Each world tracks its own checkpoints and player selections. Checkpoint data is stored in `checkpoints.properties` in the plugin's data directory.

## Protected mining test

1. With the flat test world closed, change `"GameplayConfig": "Default"` to `"GameplayConfig": "DescentInfernoMiningTest"` in the save's `universe/worlds/default/config.json`.
2. Open the world and run `/gamemode creative`. Place **Descent Inferno Iron Node** next to vanilla iron ore and stone. Put a copper pickaxe and a spare block in your inventory.
3. Run `/gamemode adventure`. The copper pickaxe should mine the Descent Inferno node and leave the vanilla iron and stone intact. Normal block placement should fail.
4. Run `/gamemode creative` to place or break blocks again in the same world.

Mining the node adds one iron ore directly to your inventory and turns the node into depleted stone. After 15 seconds it becomes mineable again. If your inventory cannot accept the ore, the node stays ready.

Try mining two nodes a few seconds apart to check that their cooldowns are independent. Also try mining with no room for iron. Cooldowns are currently kept in memory. If you close the world while a node is depleted, replace that block in Creative to reset it.

## Remaining design

The next gameplay systems are regional resources and professions, city markets with remote price visibility, and risky travel between cities. The design also calls for travel encounters, PvP and bandit rules, and daily Adventurers Guild quests ranked Wheat through Platinum.

## Teams

Grant builders `descentinferno.team.assign` to manage teams. Use `/descentinfernoteam player red <player>` to assign a player, or stand next to an NPC and use `/descentinfernoteam npc blue` to assign the closest NPC within six blocks. Team names are created on assignment. Use `none` to clear an assignment and `/descentinfernoteam status` to see your team. Membership is saved by entity ID. An NPC spawned as a replacement has its own ID.

Assigned teammates cannot damage one another. Different teams are hostile for NPC targeting, subject to what the NPC's role can do. Unassigned NPCs keep their native behavior. To test enemy players fighting, enable `IsPvpEnabled` in the test world's config. Keep both players in Adventure.

## Markets

1. As a builder with `descentinferno.market.admin`, create a market with `/descentinfernomarket create quarry`.
2. Place a **Descent Inferno Trading Post** in Creative. Aim at it from within six blocks and run `/descentinfernomarket bind quarry`. The command reports the post coordinates. You can also supply `<x> <y> <z>` after the market ID. Posts assigned to the same market share listings.
3. Give the test buyer some `DescentInferno_Coin` with `/give DescentInferno_Coin --quantity=20`. Coins stack to 1,000. For this test, builders supply the coins.
4. The seller holds an item in their active hotbar slot and uses the post. Enter the quantity and total coin price, then select **List item**. The buyer uses any post for that market and selects **Buy**.
5. The seller uses **Claim coins** at a post for the same market. They can also cancel their own listings from the post. Listings and proceeds persist across normal restarts.

Use `/descentinfernomarket list` to see all market IDs and post counts. Use `/descentinfernomarket delete <market>` to delete an empty market and clear its post assignments. Active listings and unclaimed coins prevent deletion or reassignment of the market's last post. All market commands also work with `/descentmarket`.

## Ore progression

Use Descent Inferno ore nodes in Creative. Item IDs follow `DescentInferno_Ore_<Metal>_<Host>`, for example `DescentInferno_Ore_Copper_Stone` or `DescentInferno_Ore_Iron_Basalt`. The original `DescentInferno_Iron_Node` remains the iron-in-stone variant. Nodes come in stone and basalt, with Prisma available in stone. All nodes give one ore and regenerate after 15 seconds. They use the same tier rule regardless of host appearance.

Before updating an existing test world, replace retired nodes in Creative with the same mineral in stone or basalt. For example, use basalt for cracked basalt and stone for calcite. Replace depleted blocks from retired hosts as well.

| Pickaxe | Highest tier it can mine |
| --- | --- |
| Wood, Crude, Scrap | Copper |
| Copper | Iron and Silver |
| Iron | Thorium and Gold |
| Thorium | Cobalt |
| Cobalt | Adamantite |
| Adamantite | Mithril |
| Mithril | Onyxium |
| Onyxium | Prisma |

A pickaxe can also mine all lower tiers. These are Descent Inferno prototype balance choices.
