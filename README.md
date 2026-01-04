# Package Logger - Minecraft Fabric Mod

A deep packet logging mod for Minecraft 1.21.4 that captures all network traffic with full NBT/Component data.

## Features

### Deep Packet Logging

- **Full NBT/Component Data**: Unlike simple loggers, this mod extracts ALL data from packets
- **ItemStack Details**: Item ID, count, enchantments, lore, custom data, attributes
- **Container Format**: Inventory packets logged in Minecraft NBT-style format
- **Specialized Unpackers**: Custom handlers for 15+ packet types

### Supported Packet Types

| Packet                             | Logged Data                                 |
| ---------------------------------- | ------------------------------------------- |
| `InventoryS2CPacket`               | Full container contents with slot positions |
| `ScreenHandlerSlotUpdateS2CPacket` | Slot ID + complete item data                |
| `ClickSlotC2SPacket`               | Action type, modified slots, cursor stack   |
| `BlockEntityUpdateS2CPacket`       | Position, type, complete NBT                |
| `EntityTrackerUpdateS2CPacket`     | Entity ID, type, all DataTracker values     |
| `EntityAttributesS2CPacket`        | All attributes with modifiers               |
| `ChunkDataS2CPacket`               | BlockEntity NBTs                            |
| And more...                        |                                             |

### Session-Based Logging

- New log file created on world/server join
- New log file when logging is re-enabled
- Filename includes timestamp and world/server name

### Config Screen

- **Keybind**: Press `R` to open config
- **Packet Selection**: Dual-list selector for S2C and C2S packets
- **Log Mode**: Chat or File output
- **Toggle**: Enable/disable logging

## Installation

### Requirements

- Minecraft 1.21.4
- Fabric Loader 0.16.0+
- Fabric API
- Java 21

### Build

```bash
./gradlew build
```

Output: `build/libs/package-logger-1.0.0.jar`

## Usage

### Open Config

Press `R` in-game or use the keybind settings.

### Log Files

Logs are saved to:

```
.minecraft/config/package-logger/packets_2026-01-04_15-30-45_servername.log
```

### Example Output

```
[12:34:56.789] [S2C] InventoryS2CPacket {syncId:2,revision:1,id:"minecraft:generic_9x3",components:{"minecraft:container":[{item:{id:"minecraft:diamond_sword",count:1,components:{"minecraft:enchantments":{levels:{"minecraft:sharpness":5}}}},slot:0}]}}
```

## Configuration

Config saved at: `.minecraft/config/package-logger-config.json`

```json
{
  "logPackages": true,
  "logMode": "FILE",
  "deepLogging": true,
  "selectedS2CPackages": ["InventoryS2CPacket"],
  "selectedC2SPackages": ["ClickSlotC2SPacket"]
}
```

## Project Structure

```
src/client/java/dev/redstone/packagelogger/
├── PackageLoggerClient.java       # Client entrypoint
├── config/
│   └── ModConfig.java             # Configuration
├── logger/
│   ├── PacketLogger.java          # Main logger
│   └── unpacker/                  # Specialized packet unpackers
│       ├── ItemStackFormatter.java
│       ├── InventoryS2CUnpacker.java
│       └── ...
├── mixin/client/
│   ├── ClientConnectionMixin.java # Packet interception
│   └── ...
└── screen/
    └── SimpleConfigScreen.java    # Config UI
```

## License

MIT

## Credits

- Fabric API Team
- Minecraft Modding Community
