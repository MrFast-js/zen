package meowing.zen.feats.rift

import meowing.zen.Zen
import meowing.zen.config.ConfigDelegate
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.ChatEvent
import meowing.zen.events.EntityEvent
import meowing.zen.events.RenderEvent
import meowing.zen.events.WorldEvent
import meowing.zen.feats.Feature
import meowing.zen.utils.ItemUtils.isHolding
import meowing.zen.utils.Render3D
import meowing.zen.utils.Utils.removeFormatting
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import java.awt.Color

@Zen.Module
object LarvaSilkLines : Feature("larvasilklines", area = "the rift") {
    private var startingSilkPos: BlockPos? = null
    private val larvasilklinescolor by ConfigDelegate<Color>("larvasilklinescolor")

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Rift", "Larva silk display", ConfigElement(
                "larvasilklines",
                "Larva silk lines display",
                ElementType.Switch(false)
            ), isSectionToggle = true)
            .addElement("Rift", "Larva silk display", "Color", ConfigElement(
                "larvasilklinescolor",
                "Colorpicker",
                ElementType.ColorPicker(Color(0, 255, 255, 127))
            ))
    }

    override fun initialize() {
        createCustomEvent<RenderEvent.World>("render") { event ->
            if (startingSilkPos == null) return@createCustomEvent
            if (isHolding("LARVA_SILK")) {
                val lookingAt: MovingObjectPosition? = player?.rayTrace(4.0, event.partialTicks)
                Render3D.drawSpecialBB(startingSilkPos!!, larvasilklinescolor, event.partialTicks)

                if (lookingAt?.blockPos != null && lookingAt.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    val pos = startingSilkPos!!
                    val lookingAtPos = lookingAt.blockPos
                    val start = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
                    val finish = Vec3(lookingAtPos.x + 0.5, lookingAtPos.y + 0.5, lookingAtPos.z + 0.5)

                    Render3D.drawLine(start, finish, 2f, larvasilklinescolor, event.partialTicks)
                    Render3D.drawSpecialBB(lookingAtPos, larvasilklinescolor, event.partialTicks)
                }
            }
        }

        register<ChatEvent.Receive> { event ->
            if (event.event.message.unformattedText.removeFormatting().startsWith("You cancelled the wire")) {
                startingSilkPos = null
                unregisterEvent("render")
            }
        }

        register<EntityEvent.Interact> { event ->
            if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && isHolding("LARVA_SILK")) {
                if (startingSilkPos == null) {
                    startingSilkPos = event.pos
                    registerEvent("render")
                    return@register
                }
                startingSilkPos = null
                unregisterEvent("render")
            }
        }

        register<WorldEvent.Change> {
            startingSilkPos = null
            unregisterEvent("render")
        }
    }
}