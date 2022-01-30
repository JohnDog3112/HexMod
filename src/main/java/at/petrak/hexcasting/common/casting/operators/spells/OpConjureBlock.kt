package at.petrak.hexcasting.common.casting.operators.spells

import at.petrak.hexcasting.api.Operator.Companion.getChecked
import at.petrak.hexcasting.api.RenderedSpell
import at.petrak.hexcasting.api.SpellDatum
import at.petrak.hexcasting.api.SpellOperator
import at.petrak.hexcasting.common.blocks.BlockConjured
import at.petrak.hexcasting.common.blocks.HexBlocks
import at.petrak.hexcasting.common.casting.CastingContext
import at.petrak.hexcasting.common.casting.colors.CapPreferredColorizer
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

object OpConjureBlock : SpellOperator {
    override val argc = 1
    override fun execute(args: List<SpellDatum<*>>, ctx: CastingContext): Triple<RenderedSpell, Int, List<Vec3>> {
        val target = args.getChecked<Vec3>(0)
        ctx.assertVecInRange(target)

        return Triple(
            Spell(target),
            10_000,
            listOf(target)
        )
    }

    private data class Spell(val target: Vec3) : RenderedSpell {
        override fun cast(ctx: CastingContext) {
            if (ctx.world.getBlockState(BlockPos(target)).isAir) {
                ctx.world.setBlock(BlockPos(target), HexBlocks.CONJURED.get().defaultBlockState(), 2)

                val otherHandItem = ctx.caster.getItemInHand(ctx.otherHand)
                if (ctx.world.getBlockState(BlockPos(target)).block is BlockConjured && CapPreferredColorizer.isColorizer(otherHandItem.item)) {
                    BlockConjured.setColor(ctx.world, BlockPos(target), otherHandItem)
                }
            }
        }
    }
}