package com.Panzimy.mixin;

import io.netty.buffer.ByteBuf;
import org.mtr.packet.PacketBufferReceiver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PacketBufferReceiver.class)
public abstract class FixInt {

    @Shadow
    private int readIndex;

    @Shadow
    private ByteBuf[] byteBufArray;

    @Unique
    private byte mtrfix$readByteAcrossChunks() {
        while (readIndex < byteBufArray.length
                && byteBufArray[readIndex].readableBytes() == 0) {
            readIndex++;
        }

        if (readIndex >= byteBufArray.length) {
            throw new IndexOutOfBoundsException(
                    "Unexpected end of MTR packet chunks"
            );
        }

        return byteBufArray[readIndex].readByte();
    }

    @Unique
    private int mtrfix$readIntAcrossChunks() {
        return ((mtrfix$readByteAcrossChunks() & 0xFF) << 24)
                | ((mtrfix$readByteAcrossChunks() & 0xFF) << 16)
                | ((mtrfix$readByteAcrossChunks() & 0xFF) << 8)
                |  (mtrfix$readByteAcrossChunks() & 0xFF);
    }

    @Unique
    private long mtrfix$readLongAcrossChunks() {
        return ((long) mtrfix$readIntAcrossChunks() << 32)
                | (mtrfix$readIntAcrossChunks() & 0xFFFFFFFFL);
    }

    @Redirect(
            method = "readInt()I",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/netty/buffer/ByteBuf;readInt()I"
            )
    )
    private int mtrfix$redirectReadInt(ByteBuf buffer) {
        return mtrfix$readIntAcrossChunks();
    }

    @Redirect(
            method = "readLong()J",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/netty/buffer/ByteBuf;readLong()J"
            )
    )
    private long mtrfix$redirectReadLong(ByteBuf buffer) {
        return mtrfix$readLongAcrossChunks();
    }
}