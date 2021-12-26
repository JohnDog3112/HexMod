package at.petrak.hex.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server->client when the player finishes casting a spell.
 */
public record MsgNewSpellPatternAck(int windowID, boolean quitCasting) {
    public static MsgNewSpellPatternAck deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        var windowID = buf.readInt();
        var quitCasting = buf.readBoolean();
        return new MsgNewSpellPatternAck(windowID, quitCasting);
    }

    public void serialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        buf.writeInt(this.windowID);
        buf.writeBoolean(this.quitCasting);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    if (ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
                        return;
                    }

                    if (quitCasting) {
                        Minecraft.getInstance().setScreen(null);
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }

}
