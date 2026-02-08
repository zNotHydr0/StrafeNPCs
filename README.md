# ⚔️ StrafeNPCs

![Java](https://img.shields.io/badge/Java-8-orange?style=flat-square) ![Spigot](https://img.shields.io/badge/Spigot-1.8.8-yellow?style=flat-square) ![Status](https://img.shields.io/badge/Status-Stable-brightgreen?style=flat-square) ![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

**StrafeNPCs** is an ultra-lightweight and optimized NPC management plugin. Originally designed for **Spigot 1.8.8**, it is built using **Java Reflection** to maintain compatibility with newer versions.

Developed for **[strafeland.club](https://strafeland.club)**.

---

## 🚀 Key Features

* **⚡ Zero Lag:** NPCs are generated via **Packets**, meaning they are not real server entities. This ensures maximum performance even with hundreds of NPCs.
* **📡 Netty Injection:** Uses a custom `PacketReader` injected into the player's network channel to detect clicks (Interactions) instantly.
* **🎨 Dynamic Skins:** Fetches skins directly from the Mojang API asynchronously.
* **👻 Hidden Nametags:** Uses Scoreboard Teams to hide the NPC's *nametag*, offering a clean and professional look.
* **🛡️ Edit Mode:** Secure selection system to avoid accidentally modifying the wrong NPC.
* **💾 Persistence:** All data is saved in `saves.yml` and persists after restarts.
* **🌍 Multi-world:** Automatic support for NPC teleportation and respawning when changing dimensions or worlds.

---

## 🛠️ Commands

The main command is `/npc` (alias `/npcs`).

| Command | Description |
| :--- | :--- |
| `/npc create <name>` | Creates a new NPC at your current location and automatically selects it. |
| `/npc delete <name>` | Permanently deletes an existing NPC. |
| `/npc edit <name>` | Selects an NPC for editing (required for skin/cmd). |
| `/npc skin <skin_name>` | Changes the skin of the selected NPC (Requires `/npc edit`). |
| `/npc cmd <command>` | Assigns a command for the player to execute upon clicking (without `/`). |
| `/npc tp <name>` | Teleports you to the exact location of the NPC. |
| `/npc tphere <name>` | Brings the NPC to your current location. |
| `/npc look <name>` | Makes the NPC rotate its body and head to look at you. |
| `/npc debug` | (OP only) Forces packet re-injection if a network error occurs. |

---

## ⚙️ Configuration

### `messages.yml`
All messages are 100% configurable and support color codes (`&`).

```yaml
chat-messages:
  prefix: "&8[&bStrafeNPCs&8] "
  npc-created: "&aNPC &e%name% &acreated successfully."
  skin-updated: "&aSkin updated successfully."
  npc-selected: "&aYou have selected NPC &e%name%&a."
  # ... and much more.
````

## 🔧 Installation

1.  Download the `.jar` file from the **Releases** section (or compile it yourself).
2.  Place it in your server's `/plugins/` folder.
3.  Restart the server.
4.  Done! Configure messages to your liking in `plugins/StrafeNPCs/messages.yml`.

---

## 👨‍💻 Author

Created by zNotHydr0 :)
