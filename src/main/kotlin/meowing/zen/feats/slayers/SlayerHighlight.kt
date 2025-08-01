package meowing.zen.feats.slayers

import meowing.zen.Zen
import meowing.zen.config.ConfigDelegate
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.RenderEvent
import meowing.zen.feats.Feature
import meowing.zen.utils.OutlineUtils
import net.minecraft.entity.EntityLivingBase
import java.awt.Color

@Zen.Module
object SlayerHighlight : Feature("slayerhighlight") {
    private var cachedEntity: EntityLivingBase? = null
    private var lastBossId = -1
    private val slayerhighlightcolor by ConfigDelegate<Color>("slayerhighlightcolor")
    private val slayerhighlightwidth by ConfigDelegate<Double>("slayerhighlightwidth")

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Slayers", "Slayer highlight", ConfigElement(
                "slayerhighlight",
                "Slayer highlight",
                ElementType.Switch(false)
            ), isSectionToggle = true)
            .addElement("Slayers", "Slayer highlight", "Color", ConfigElement(
                "slayerhighlightcolor",
                "Slayer highlight color",
                ElementType.ColorPicker(Color(0, 255, 255, 127))
            ))
            .addElement("Slayers", "Slayer highlight", "Width", ConfigElement(
                "slayerhighlightwidth",
                "Slayer highlight width",
                ElementType.Slider(1.0, 10.0, 2.0, false)
            ))
    }

    override fun initialize() {
        register<RenderEvent.EntityModel> { event ->
            if (!SlayerTimer.isFighting || SlayerTimer.BossId == -1) {
                cachedEntity = null
                lastBossId = -1
                return@register
            }

            if (cachedEntity == null || lastBossId != SlayerTimer.BossId) {
                cachedEntity = world?.getEntityByID(SlayerTimer.BossId) as? EntityLivingBase
                lastBossId = SlayerTimer.BossId
            }

            if (event.entity == cachedEntity)
                OutlineUtils.outlineEntity(
                    event = event,
                    color = slayerhighlightcolor,
                    lineWidth = slayerhighlightwidth.toFloat(),
                    shouldCancelHurt = true
                )
        }
    }
}