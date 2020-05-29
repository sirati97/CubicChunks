package cubicchunks.cc.mixin.core.common.ticket;

import cubicchunks.cc.chunk.IChunkManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(ChunkManager.ProxyTicketManager.class)
public abstract class MixinProxyTicketManager extends MixinTicketManager {

    @Shadow ChunkManager this$0;

    @Override
    public ChunkHolder setCubeLevel(long cubePosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel) {
        return ((IChunkManager)this$0).setCubeLevel(cubePosIn, newLevel, holder, oldLevel);
    }

    @Override
    public boolean containsCubes(long cubePos) {
        return ((IChunkManager)this$0).getUnloadableCubes().contains(cubePos);
    }

    @Override
    public ChunkHolder getCubeHolder(long cubePosIn) {
        return ((IChunkManager)this$0).getCubeHolder(cubePosIn);
    }
}
