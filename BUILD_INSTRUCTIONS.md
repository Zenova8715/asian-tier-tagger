# AsianTierTagger — Build Instructions

## Prerequisites

| Tool        | Version     |
|-------------|-------------|
| Java JDK    | 17 or 21    |
| Gradle      | 8.4+ (via wrapper — no install needed) |
| Minecraft   | 1.20.1      |

---

## Project Structure

```
asiantier-tagger/
├── build.gradle                          Gradle build script
├── gradle.properties                     Version pins (MC, Fabric, mod)
├── settings.gradle                       Project name + plugin repos
├── gradle/wrapper/gradle-wrapper.properties
│
├── src/main/java/com/asiantiertagger/
│   ├── AsianTierTaggerClient.java        ← Mod entrypoint (client-side init)
│   │
│   ├── api/
│   │   ├── TierApiManager.java           ← HTTP fetching, retry logic
│   │   └── PlayerTierData.java           ← Data record (player/tier/color/prefix)
│   │
│   ├── cache/
│   │   └── PlayerCacheManager.java       ← In-memory TTL cache
│   │
│   ├── config/
│   │   ├── ModConfig.java                ← JSON config file (load/save)
│   │   └── ConfigScreen.java             ← In-game settings GUI
│   │
│   ├── commands/
│   │   └── TierCommand.java              ← /asiantiers command tree
│   │
│   ├── render/
│   │   └── TierRenderSystem.java         ← Tier → colored Text builder
│   │
│   └── mixin/
│       ├── PlayerEntityRendererMixin.java ← Above-head nametag injection
│       ├── PlayerListHudMixin.java        ← Tab list injection
│       └── ChatHudMixin.java              ← Chat message injection
│
├── src/main/resources/
│   ├── fabric.mod.json                   Mod metadata
│   └── asiantiertagger.mixins.json       Mixin class list
│
└── example-api/
    └── server.js                         Node.js example API server
```

---

## Step 1 — Configure the Gradle Wrapper

The wrapper downloads Gradle automatically. No Gradle install needed.

**Linux / macOS:**
```bash
chmod +x gradlew
```

**Windows:** use `gradlew.bat` instead of `./gradlew` in all commands below.

---

## Step 2 — Build the Mod

```bash
./gradlew build
```

This compiles the mod, runs Mixin annotation processing, and packages the JAR.

The compiled mod JAR will be at:
```
build/libs/asiantiertagger-1.0.0.jar
```

> There will also be a `*-sources.jar` — you only need the main JAR.

---

## Step 3 — Install the Mod

1. Install **Fabric Loader 0.14.22** for Minecraft 1.20.1 from [fabricmc.net](https://fabricmc.net/use/installer/)
2. Download **Fabric API 0.91.0+1.20.1** from [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3. Copy both JARs to your `.minecraft/mods/` folder:
   - `asiantiertagger-1.0.0.jar`
   - `fabric-api-0.91.0+1.20.1.jar`
4. Launch Minecraft using the **Fabric** profile

---

## Step 4 — Configure the API URL

On first launch the mod creates:
```
.minecraft/config/asiantiertagger.json
```

Edit it and set your API URL:
```json
{
  "enabled": true,
  "apiUrl": "https://your-tier-api.com",
  "refreshIntervalSeconds": 30,
  "showAboveHead": true,
  "showInTab": true,
  "showInChat": true,
  "hideOwnTag": false,
  "gamemodeFilter": ""
}
```

Or set it in-game:
```
/asiantiers seturl https://your-tier-api.com
```

---

## Step 5 — Run the Example API (optional)

```bash
cd example-api
npm install express
node server.js
# API running at http://localhost:3000
```

Test it:
```bash
curl http://localhost:3000/player/VoidFury
```

Expected response:
```json
{
  "player": "VoidFury",
  "tier": "HT2",
  "gamemode": "NethPot",
  "color": "#AA0000",
  "prefix": "[HT2]"
}
```

---

## In-Game Commands

| Command                          | Description                            |
|----------------------------------|----------------------------------------|
| `/asiantiers toggle`             | Enable / disable the mod               |
| `/asiantiers config`             | Open the in-game settings GUI          |
| `/asiantiers refresh`            | Clear cache and re-fetch all players   |
| `/asiantiers lookup <name>`      | Force-fetch a specific player's tier   |
| `/asiantiers seturl <url>`       | Set the API URL at runtime             |
| `/asiantiers interval <seconds>` | Change the auto-refresh interval       |
| `/asiantiers status`             | Print current settings to chat         |

---

## Tier Color Reference

| Tier      | Color       | Minecraft Formatting |
|-----------|-------------|----------------------|
| LT5       | Gray        | `§7`                 |
| LT4       | Dark Gray   | `§8`                 |
| LT3       | Green       | `§a`                 |
| LT2       | Aqua        | `§b`                 |
| LT1       | Blue        | `§9`                 |
| HT5       | Yellow      | `§e`                 |
| HT4       | Gold        | `§6`                 |
| HT3       | Red         | `§c`                 |
| HT2       | Dark Red    | `§4`                 |
| HT1       | Dark Purple | `§5`                 |

---

## API Contract

```
GET /player/{minecraft_username}
```

**Success (200):**
```json
{
  "player":   "VoidFury",
  "tier":     "HT2",
  "gamemode": "NethPot",
  "color":    "#FF5555",
  "prefix":   "[HT2]"
}
```

**Not found (404):** Player has no tier — mod displays plain username.

**Any other error / timeout:** Mod retries up to 3 times with exponential back-off (1s → 2s → 4s), then caches a "no tier" sentinel to avoid hammering the API.

---

## Common Issues

| Problem | Solution |
|---------|----------|
| Tags not showing | Check `/asiantiers status` — verify `enabled: true` and API URL is set |
| API unreachable | Check server logs; mod will fall back to plain names silently |
| FPS drops | Verify the executor is running (should be daemon threads — check game logs) |
| Tags wrong after server switch | Run `/asiantiers refresh` to clear the cache |
| Build fails: `Could not resolve fabric-loom` | Make sure `settings.gradle` includes `maven { url = 'https://maven.fabricmc.net/' }` |

---

## Rebuilding After Code Changes

```bash
./gradlew build
```

No clean needed unless you change Mixin targets — in that case:
```bash
./gradlew clean build
```
