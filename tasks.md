# Descent Inferno prototype checklist

## 1. Set up the test and choose what to reuse

- [x] Load Descent Inferno in a flat Hytale test world and verify that `/descentinfernocity status` runs.
- [x] Review Hytale 0.7 tools and mods. Start with Hytale's Asset Editor, Builder Tools, and NPC spawn markers. Shortlist WorldProtect for regions, Kindred or Dominion Factions for teams, and Better Shop/Auction/OfferHouse for trade. Verify any candidate in a separate 0.7.6 test world before adopting it.
- [x] Inspect the installed 0.7.6 game modes and world rules. Adventure is the normal player mode and Creative is the builder mode. There is no separate built-in Survival mode.
- [ ] In the flat test world, verify that Descent Inferno allows Creative players to build and break blocks, and limits Adventure players to mining designated nodes. Check placement, chest and door interactions, and mode switching without reopening the world.

## 2. Build one complete mining interaction

- [ ] Protect ordinary blocks in the test world. Verify that builders can still edit the map and regular players can mine designated resource nodes.
- [ ] Make one placeable iron node. When mined, it gives one reward and becomes depleted rock in the same spot. It returns to iron when its cooldown ends.
- [ ] Test two nodes and two players. Each node should have its own cooldown, and two players mining the same node should only get one reward between them. Use a short test cooldown before trying a gameplay value such as five minutes.
- [ ] Decide what happens to a depleted node when players leave the area or the server restarts.

## 3. Build the first playable trip

- [ ] Block out a safe camp, a short route, and a danger area with one enemy spawn. Place the first resource nodes by hand.
- [ ] Define the zone rules. In safe zones, players cannot harm each other. In danger zones, opposing players can fight and allies cannot harm one another. Decide how neutral players, NPCs, and projectiles crossing a boundary behave.
- [ ] Test the route with the mining loop. Give it readable terrain, a clear path, and deliberate cover or elevation. See whether a player can make an ambush, escape, or hold a position without the terrain making the fight hard to read.
- [ ] Try one travel decision. For example, offer a shorter route past an enemy camp and a longer route with better cover. Add a random encounter or weather hazard once the route itself gives players a reason to choose.
- [ ] Decide where players respawn and what death costs on this map. Revisit the existing checkpoint and armor-drop prototype once we know what we want players to risk on a trip.
- [ ] Compare a continuous overworld with instanced areas after playing the route. Consider whether players should share nodes, run into rival teams, and fight over the same objectives. Try a separate instance if a dungeon or other activity needs its own space.

## 4. Set the rules for loot, crafting, and combat

- [ ] Pick one early recipe with an obvious benefit, such as a basic health potion or a meal with a combat bonus. Work backward to the few ingredients and sources it needs.
- [ ] Start with low drop quantities. Track how much time and risk it takes to make the first useful item. Use “enough for one potion in roughly ten minutes” as a test target and increase drops only if playtesting shows a need.
- [ ] Give each drop a clear purpose before adding it. Reuse ingredients where it makes sense and avoid near-identical materials competing for inventory space.
- [ ] Try one active weapon choice per expedition. Compare locking the choice until players return to safety with allowing a swap outside combat.
- [ ] Test starter gear against one upgrade. Check whether the upgrade feels worth earning and whether a skilled starter can still win through positioning, timing, or a better decision.

## 5. Add currency and one market

- [ ] Define what creates money and what removes it from circulation. Test one reliable way to earn it and one reason to spend it.
- [ ] Build or integrate one market where players can list an item, filter listings, and buy with currency. Verify that purchases transfer the item and money exactly once.
- [ ] Test a few real trades using the small item pool. Consider separate city markets and remote price browsing once there’s a reason to carry goods between cities.

## 6. Try farming and territory as separate experiments

- [ ] Test one crop on designated fertile ground. Decide who can plant and harvest it, and give the crop an immediate use in a recipe.
- [ ] Give players a renewable source of seeds. Try occasional seed spawns on fertile ground and decide whether public plots and personal plots should have different rules.
- [ ] Build one small fort inside a danger zone. Give it a few readable approaches, useful cover, and a tower or archer position.
- [ ] Test capture with two teams. Start progress when attackers have more players inside the capture area. Pause on a tie. Decide whether progress pauses, decays, or reverses when attackers lose the advantage.
- [ ] Give the holding team NPC guards. Test how guards behave when no defenders are online, how a fort changes hands, and whether a respawn delay around ten minutes gives attackers a fair opening. Start by counting players only toward capture progress.

## Remaining after the teams, commerce, and ore build

- [ ] Add non-metal resources and decide which are nodes, harvestables, or enemy drops.
- [ ] Set and test resource and NPC spawn rates.
- [ ] Build a town with themed shops and crafting stations.
- [ ] Add NPC vendors that sell goods for Descent Inferno Coins.
- [ ] Implement safe zones where players take no damage, including damage from enemies.
- [ ] Prototype town, danger zone, and second town as separate instances. Verify that safe instances override team hostility and compare travel and multiplayer visibility with a shared world.
