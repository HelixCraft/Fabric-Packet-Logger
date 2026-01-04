package dev.redstone.rendertweaks.logger;

import dev.redstone.rendertweaks.config.ModConfig;
import dev.redstone.rendertweaks.logger.unpacker.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Deep Packet Logger - Loggt alle Netzwerk-Pakete mit vollständigen Daten.
 * Verwendet spezialisierte Unpacker für verschiedene Paket-Typen.
 * 
 * Erstellt ein neues Log-File bei:
 * - Welt/Server Join
 * - Aktivierung von logPackages
 */
public class PacketLogger {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private static Path currentLogFile = null;
    private static String currentSessionId = null;
    private static boolean wasLoggingEnabled = false;
    
    // Registry für spezialisierte Packet-Unpacker
    private static final Map<Class<?>, PacketUnpacker<?>> UNPACKERS = new HashMap<>();
    
    static {
        // Registriere alle spezialisierten Unpacker
        registerUnpackers();
    }
    
    private static void registerUnpackers() {
        // Inventory/Item Pakete
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.play.InventoryS2CPacket"), new InventoryS2CUnpacker());
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket"), new SlotUpdateS2CUnpacker());
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket"), new CreativeInventoryC2SUnpacker());
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket"), new ClickSlotC2SUnpacker());
        
        // Block Pakete
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket"), new BlockEntityUpdateS2CUnpacker());
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket"), new BlockUpdateS2CUnpacker());
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket"), new ChunkDeltaUpdateS2CUnpacker());
        
        // Entity Pakete
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket"), new EntityTrackerUpdateS2CUnpacker());
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket"), new EntityAttributesS2CUnpacker());
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket"), new EntitySpawnS2CUnpacker());
        
        // Chunk Pakete
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket"), new ChunkDataS2CUnpacker());
        
        // NBT/Custom Pakete
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.play.NbtQueryResponseS2CPacket"), new NbtQueryResponseS2CUnpacker());
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket"), new CustomPayloadS2CUnpacker());
        UNPACKERS.put(getPacketClass("net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket"), new CustomPayloadC2SUnpacker());
    }
    
    private static Class<?> getPacketClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    /**
     * Wird aufgerufen wenn der Spieler einer Welt/Server beitritt.
     * Erstellt ein neues Log-File für diese Session.
     */
    public static void onWorldJoin(String worldName) {
        if (ModConfig.getInstance().logMode == ModConfig.LogMode.FILE) {
            currentSessionId = null; // Force new file
            currentLogFile = null;
            System.out.println("[PacketLogger] New session started: " + worldName);
        }
    }
    
    /**
     * Wird aufgerufen wenn der Spieler die Welt/Server verlässt.
     */
    public static void onWorldLeave() {
        if (currentLogFile != null && ModConfig.getInstance().logMode == ModConfig.LogMode.FILE) {
            // Schreibe Session-Ende in Log
            try {
                synchronized (PacketLogger.class) {
                    try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(currentLogFile.toFile(), true)))) {
                        writer.println();
                        writer.println("=== Session ended: " + LocalDateTime.now().format(FILE_DATE_FORMAT) + " ===");
                    }
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        currentSessionId = null;
        currentLogFile = null;
    }
    
    public static void logIncoming(Packet<?> packet) {
        logPacket(packet, true);
    }
    
    public static void logOutgoing(Packet<?> packet) {
        logPacket(packet, false);
    }
    
    private static void logPacket(Packet<?> packet, boolean incoming) {
        ModConfig config = ModConfig.getInstance();
        
        // Check if logging was just enabled -> new file
        if (config.logPackages && !wasLoggingEnabled && config.logMode == ModConfig.LogMode.FILE) {
            currentSessionId = null;
            currentLogFile = null;
            System.out.println("[PacketLogger] Logging enabled - starting new session");
        }
        wasLoggingEnabled = config.logPackages;
        
        if (!config.logPackages) {
            return;
        }
        
        String simpleName = packet.getClass().getSimpleName();
        
        if (incoming) {
            if (!shouldLogS2C(simpleName, config)) return;
        } else {
            if (!shouldLogC2S(simpleName, config)) return;
        }
        
        String timestamp = LocalTime.now().format(TIME_FORMAT);
        String direction = incoming ? "S2C" : "C2S";
        
        // Deep Unpack des Pakets
        String packetData = unpackPacket(packet);
        
        if (config.logMode == ModConfig.LogMode.CHAT) {
            logToChat(timestamp, direction, simpleName, packetData, incoming);
        } else {
            logToFile(timestamp, direction, simpleName, packetData);
        }
    }
    
    private static boolean shouldLogS2C(String simpleName, ModConfig config) {
        if (config.selectedS2CPackages.isEmpty()) return false;
        for (String selected : config.selectedS2CPackages) {
            if (simpleName.equals(selected) || simpleName.endsWith(selected)) return true;
        }
        return false;
    }
    
    private static boolean shouldLogC2S(String simpleName, ModConfig config) {
        if (config.selectedC2SPackages.isEmpty()) return false;
        for (String selected : config.selectedC2SPackages) {
            if (simpleName.equals(selected) || simpleName.endsWith(selected)) return true;
        }
        return false;
    }
    
    /**
     * Entpackt ein Paket mit dem passenden Unpacker oder Reflection-Fallback.
     */
    @SuppressWarnings("unchecked")
    private static String unpackPacket(Packet<?> packet) {
        try {
            // Suche spezialisierten Unpacker
            PacketUnpacker<Packet<?>> unpacker = (PacketUnpacker<Packet<?>>) UNPACKERS.get(packet.getClass());
            if (unpacker != null) {
                return unpacker.unpack(packet);
            }
            
            // Fallback: Reflection-basiertes Deep Logging
            return ReflectionUnpacker.unpackWithReflection(packet);
            
        } catch (Exception e) {
            return "{error: \"" + e.getMessage() + "\"}";
        }
    }
    
    private static void logToChat(String timestamp, String direction, String packetName, String packetData, boolean incoming) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null || client.inGameHud.getChatHud() == null) return;
        
        MutableText timeText = Text.literal("[" + timestamp + "] ").formatted(Formatting.GRAY);
        MutableText dirText = Text.literal("[" + direction + "] ").formatted(incoming ? Formatting.GREEN : Formatting.RED);
        MutableText nameText = Text.literal(packetName + " ").formatted(Formatting.YELLOW);
        
        String shortData = packetData.length() > 300 ? packetData.substring(0, 300) + "..." : packetData;
        MutableText dataText = Text.literal(shortData).formatted(Formatting.WHITE);
        
        MutableText fullMessage = Text.empty().append(timeText).append(dirText).append(nameText).append(dataText);
        client.inGameHud.getChatHud().addMessage(fullMessage);
    }
    
    private static void logToFile(String timestamp, String direction, String packetName, String packetData) {
        try {
            Path logFile = getLogFile();
            String logLine = String.format("[%s] [%s] %s %s%n", timestamp, direction, packetName, packetData);
            
            synchronized (PacketLogger.class) {
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile.toFile(), true)))) {
                    writer.print(logLine);
                }
            }
        } catch (IOException e) {
            System.err.println("[PacketLogger] Error writing to log file: " + e.getMessage());
        }
    }
    
    private static Path getLogFile() throws IOException {
        // Erstelle neue Session wenn nötig
        if (currentSessionId == null || currentLogFile == null) {
            currentSessionId = LocalDateTime.now().format(FILE_DATE_FORMAT);
            
            Path configDir = FabricLoader.getInstance().getConfigDir();
            Path logDir = configDir.resolve("package-logger");
            Files.createDirectories(logDir);
            
            // Welt-Name ermitteln
            String worldName = getWorldName();
            String fileName = "packets_" + currentSessionId + "_" + worldName + ".log";
            currentLogFile = logDir.resolve(fileName);
            
            // Header schreiben
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(currentLogFile.toFile())))) {
                writer.println("=== Deep Packet Logger ===");
                writer.println("Session: " + currentSessionId);
                writer.println("World: " + worldName);
                writer.println("Format: [TIME] [DIRECTION] PacketName {deep_data}");
                writer.println("==========================================");
                writer.println();
            }
            
            System.out.println("[PacketLogger] Created new log file: " + fileName);
        }
        return currentLogFile;
    }
    
    private static String getWorldName() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                // Multiplayer Server
                if (client.getCurrentServerEntry() != null) {
                    String address = client.getCurrentServerEntry().address;
                    // Sanitize für Dateinamen
                    return sanitizeFileName(address);
                }
                // Singleplayer
                if (client.getServer() != null && client.getServer().getSaveProperties() != null) {
                    String levelName = client.getServer().getSaveProperties().getLevelName();
                    return sanitizeFileName(levelName);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }
    
    private static String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        // Entferne ungültige Zeichen für Dateinamen
        return name.replaceAll("[^a-zA-Z0-9._-]", "_")
                   .replaceAll("_+", "_")
                   .substring(0, Math.min(name.length(), 50));
    }
}
