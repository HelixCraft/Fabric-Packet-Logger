package dev.redstone.rendertweaks.mixin.client;

import dev.redstone.rendertweaks.logger.PacketLogger;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin für ClientConnection um alle Pakete zu loggen.
 * Basiert auf dem Meteor Client Ansatz.
 */
@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    
    private static boolean debugLogged = false;
    
    /**
     * Intercepted alle eingehenden Pakete (Server -> Client)
     * Wird aufgerufen bevor handlePacket ausgeführt wird.
     */
    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V",
            shift = At.Shift.BEFORE
        )
    )
    private void onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (!debugLogged) {
            System.out.println("[PacketLogger] Mixin is working! First packet received: " + packet.getClass().getSimpleName());
            debugLogged = true;
        }
        try {
            String className = packet.getClass().getName();
            if (className.contains(".s2c.")) {
                PacketLogger.logIncoming(packet);
            }
        } catch (Exception e) {
            // Ignore
        }
    }
    
    /**
     * Intercepted alle ausgehenden Pakete (Client -> Server)
     */
    @Inject(
        method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V",
        at = @At("HEAD")
    )
    private void onSendPacket(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo ci) {
        try {
            String className = packet.getClass().getName();
            if (className.contains(".c2s.")) {
                PacketLogger.logOutgoing(packet);
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}
