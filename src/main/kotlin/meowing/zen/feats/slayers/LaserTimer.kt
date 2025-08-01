package meowing.zen.feats.slayers

import meowing.zen.Zen
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.EntityEvent
import meowing.zen.events.EventBus
import meowing.zen.events.RenderEvent
import meowing.zen.feats.Feature
import meowing.zen.utils.Render3D
import meowing.zen.utils.Utils.partialTicks
import net.minecraft.util.Vec3

@Zen.Module
object LaserTimer : Feature("lasertimer") {
    private var bossID = 0
    private val totaltime = 8.2
    private val renderCall: EventBus.EventCall = EventBus.register<RenderEvent.LivingEntity.Post> ({ renderString() }, false)

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Slayers", "Laser phase timer", ConfigElement(
                "lasertimer",
                "Laser phase timer",
                ElementType.Switch(false)
            ), isSectionToggle = true)
    }

    override fun initialize() {
        register<EntityEvent.Leave> { event ->
            if (event.entity.entityId == bossID) {
                bossID = 0
                renderCall.unregister()
            }
        }
    }

    fun handleSpawn(entityID: Int) {
        bossID = entityID - 3
        renderCall.register()
    }

    fun renderString() {
        val ent = world?.getEntityByID(bossID) ?: return
        if (player?.canEntityBeSeen(ent) != true) return
        val ridingentity = ent.ridingEntity ?: return
        val time = maxOf(0.0, totaltime - (ridingentity.ticksExisted / 20.0))
        val text = "§bLaser: §c${"%.1f".format(time)}"

        Render3D.drawString(text, ent.positionVector.add(Vec3(0.0, 1.0, 0.0)), partialTicks)
    }
}