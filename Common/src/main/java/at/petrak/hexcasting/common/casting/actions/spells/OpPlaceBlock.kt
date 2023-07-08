package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

// TODO: how to handle in cirles
object OpPlaceBlock : SpellAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        ctx: CastingEnvironment
    ): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        ctx.assertPosInRangeForEditing(pos)

        val blockHit = BlockHitResult(
            Vec3.atCenterOf(pos), ctx.caster?.direction ?: Direction.NORTH, pos, false
        )
        val itemUseCtx = ctx.caster?.let { UseOnContext(it, ctx.castingHand, blockHit) }
            ?: throw NotImplementedError("how to implement this")
        val placeContext = BlockPlaceContext(itemUseCtx)

        val worldState = ctx.world.getBlockState(pos)
        if (!worldState.canBeReplaced(placeContext))
            throw MishapBadBlock.of(pos, "replaceable")

        return SpellAction.Result(
            Spell(pos),
            MediaConstants.DUST_UNIT / 8,
            listOf(ParticleSpray.cloud(Vec3.atCenterOf(pos), 1.0))
        )
    }

    private data class Spell(val pos: BlockPos) : RenderedSpell {
        override fun cast(ctx: CastingEnvironment) {
            val caster = ctx.caster ?: return // TODO: Fix!

            val blockHit = BlockHitResult(
                Vec3.atCenterOf(pos), caster.direction, pos, false
            )

            val bstate = ctx.world.getBlockState(pos)
            val placeeStack = ctx.getHeldItemToOperateOn { it.item is BlockItem }?.stack
            if (placeeStack != null) {
                if (!IXplatAbstractions.INSTANCE.isPlacingAllowed(ctx.world, pos, placeeStack, ctx.caster))
                    return

                if (!placeeStack.isEmpty) {
                    // https://github.com/VazkiiMods/Psi/blob/master/src/main/java/vazkii/psi/common/spell/trick/block/PieceTrickPlaceBlock.java#L143
                    val oldStack = caster.getItemInHand(ctx.castingHand)
                    val spoofedStack = placeeStack.copy()

                    // we temporarily give the player the stack, place it using mc code, then give them the old stack back.
                    spoofedStack.count = 1
                    caster.setItemInHand(ctx.castingHand, spoofedStack)

                    val itemUseCtx = UseOnContext(caster, ctx.castingHand, blockHit)
                    val placeContext = BlockPlaceContext(itemUseCtx)
                    if (bstate.canBeReplaced(placeContext)) {
                        if (ctx.withdrawItem({ it == placeeStack }, 1, false)) {
                            val res = spoofedStack.useOn(placeContext)

                            caster.setItemInHand(ctx.castingHand, oldStack)
                            if (res != InteractionResult.FAIL) {
                                ctx.withdrawItem({ it == placeeStack }, 1, true)

                                ctx.world.playSound(
                                    ctx.caster,
                                    pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                                    bstate.soundType.placeSound, SoundSource.BLOCKS, 1.0f,
                                    1.0f + (Math.random() * 0.5 - 0.25).toFloat()
                                )
                                val particle = BlockParticleOption(ParticleTypes.BLOCK, bstate)
                                ctx.world.sendParticles(
                                    particle, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                                    4, 0.1, 0.2, 0.1, 0.1
                                )
                            }
                        } else {
                            caster.setItemInHand(ctx.castingHand, oldStack)
                        }
                    } else {
                        caster.setItemInHand(ctx.castingHand, oldStack)
                    }
                }
            }
        }
    }
}