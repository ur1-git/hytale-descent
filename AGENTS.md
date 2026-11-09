# Descent Inferno

## Rules

- Planning first. Discuss gameplay options, tradeoffs, and open questions with the user. Treat suggestions as proposals. Start implementation only after agreeing on scope and getting explicit direction to code.
- Teach as we build. Assume basic Java knowledge. Before an agreed change, explain the flow in plain language. Work in small steps and explain what each new or changed class and method does, who calls it, and how to try it in Hytale.
- Keep this file short and current. On each read, check whether any entry is obsolete or superseded. Periodically remove stale information and keep current decisions and open questions separate.

## First prototype

- Build a small test map with a safe area and a combat area where enemies spawn.
- Protect ordinary map blocks from player mining. Allow mining at designated resource nodes.
- A mined resource node stays in place and looks depleted. When its cooldown ends, restore its active appearance and make it mineable again.
- Descent Inferno ore nodes look like their host rock while depleted, give one ore directly to inventory, and restore after 15 seconds. A full inventory leaves them ready.
- Keep mining, teams, and commerce independent within one JAR. Trading posts share a market by ID. Sellers claim physical coins at a post for that market.

## Open design questions

- Decide where resource nodes go and whether cooldowns survive a restart.
- Define what the safe area protects against and choose the test map's death rules.
