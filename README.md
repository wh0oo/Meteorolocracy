# 🌦️ Meteorolocracy

**Meteorolocracy** is a Fabric server-side mod that lets players vote on the weather — democracy meets the sky!

## ⛅ What It Does

Players can initiate a vote to change the weather using in-game commands. Once a vote begins, others have a limited time to cast their vote. If a majority agrees, the weather changes server-wide.

## ✨ Features

- `/weathervote <sun|rain|thunder>` to start a weather vote
- `/weathervote vote <yes|no>` to cast your vote
- `/weathervote status` to check voting progress
- `/weathervote reset` (op-only) to cancel an active vote
- Automatically ends early if all players have voted
- Configurable vote duration, cooldowns, and threshold
- Fully server-side — no client mod needed

## 🔧 Configuration

Edit `config/meteorolocracy.json` to customize:
- `voteDuration` – how long the vote lasts (in seconds)
- `voteThreshold` – percent of players needed to pass (e.g. `0.5`)
- `playerCooldown` – how long a player must wait before starting another vote
- `messageColors` – customize broadcast colors (optional)
- `endEarlyWhenAllVoted` – whether voting ends early if everyone has voted

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/)
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Drop the Meteorolocracy `.jar` file into your server's `mods/` folder

## 🧪 Example Config Snippet

```json
{
  "voteDuration": 120,
  "voteThreshold": 0.5,
  "playerCooldown": 1800,
  "endEarlyWhenAllVoted": true
}
```

---

🌤️ Let your players decide the forecast.