# InventoryNotifier+Autosell

[![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)](https://fabricmc.net/)
[![Modrinth](https://img.shields.io/badge/Modrinth-Project-green)](https://modrinth.com/mod/inventorynotifier_autosell)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A lightweight **Fabric client-side mod** that notifies you when your inventory is full and can automatically sell items using server commands.  
Now includes an **in-game config menu** for even more customization.  

---

## ✨ Features
- 👜 Sends a notification **only once** when your inventory has no free slots.  
- 🔔 Plays a sound when your inventory is full or when items are autosold.  
- ⚡ Triggers **auto-sell** automatically when your inventory is full.  
- 💵 Supports `/sell all`, `/sell handall`, and **custom commands**.  
- 🔘 **Quick Autosell Mode Switch Button** for faster toggling between modes.  
- ⚙️ **Config Menu** → Open with <kbd>`</kbd> (customizable under Controls → IFN+Autosell).  
- 💰 **Autosell Threshold** → Set how many items trigger auto-sell.  
- 🎯 **Milestone Threshold Notifications** → Get notified at custom progress milestones.  
- 🎮 100% **client-side** — no server installation required.  

---

## ⚡ Commands
- `/autosell` → Toggles auto-sell (default: **all**).  
- `/autosell set all` → Automatically runs `/sell all` (⚠️ sells everything).  
- `/autosell set handall` → Automatically runs `/sell handall` (only the item type you’re holding).  
- `/autosell set custom <command>` → Runs your own command (e.g., `/autosell set custom pwarp base`).  
- To deactivate: retype `/autosell`.  
- `/slotnotify` → Toggles the milestone notifications.  
- `/invfull config` → Opens the config screen. `/invfull help` → Shows the command list.  

---

## 🎮 Versions
**v9** adds Minecraft **26.1 - 26.3** (Java 25). Older Minecraft versions keep using their own release; the 1.21.x source in `src/main` and its `build.gradle` are left unchanged (that build script targets Gradle 8, so build it with your own Gradle 8.x install, not with the bundled Gradle 9 wrapper, which is only for the 26.x build).

| Minecraft | Jar |
|---|---|
| 26.1.x | `IFN+AutoSell-v9+26.1.jar` |
| 26.2 - 26.3 | `IFN+AutoSell-v9+26.2.jar` |

Mod Menu is optional (it only adds a config button). To build the 26.x jars yourself run `./build-all.ps1` (JDK 25 needed to run Gradle); they end up in `dist/`.

### What changed in v9
- Ported to Minecraft 26.1 - 26.3.
- **Autosell Threshold now works.** It was saved but never used, autosell only ever started when the inventory was completely full. Now it starts as soon as that many slots are used (36 = completely full, as before).
- **Notifications switch now works.** It turns off every automatic message and sound.
- `/invfull config` opens the config screen, and the Mod Menu config button works (the entry point was never registered).
- Fixed a leak: every autosell added another permanent tick listener. It is one state machine now.
- Autosell no longer switches your milestone setting off for half a second (it could be saved that way if the game closed in that moment).
- The state is reset when you join or leave a world, and the first two seconds after joining are skipped so a not-yet-synced inventory does not trigger "inventory is empty".
- A corrupt or empty `ifn_config.json` no longer crashes startup (it is kept as `.bak`), invalid values are repaired, and the config is written safely.
- The config screen saves once when it closes instead of on every keystroke or slider step, and it uses two columns on short windows so the Done button is never covered.
- The unused Cloth Config library that was bundled into the jar is gone.

---

## 📦 Installation
1. Install [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api).  
2. Download the latest `.jar` from [Modrinth](https://modrinth.com/mod/inventorynotifier_autosell).  
3. Place the `.jar` file into your `mods` folder.  
4. Launch Minecraft and enjoy!  

---

## 📜 License
This project is licensed under the [MIT License](LICENSE).  
You are free to use, modify, and distribute this mod under the license terms.  

---

## 🤝 Contributing
Issues and pull requests are welcome!  
- 🐞 Report bugs or suggest features via [GitHub Issues](https://github.com/jcwd-source/InventoryNotifier-Autosell/issues).  
- 💬 Join the [Discord community](https://discord.gg/9FDhqkrK) for support and feedback.  
