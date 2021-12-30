package at.petrak.hex.common.casting.operators

import at.petrak.hex.api.SimpleOperator
import at.petrak.hex.common.casting.CastingContext
import at.petrak.hex.common.casting.SpellDatum

object OpUndo : SimpleOperator {
    override val argc = 1

    // Do literally nothing!
    override fun execute(args: List<SpellDatum<*>>, ctx: CastingContext): List<SpellDatum<*>> =
        emptyList()
}