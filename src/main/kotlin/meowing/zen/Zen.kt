package meowing.zen

import meowing.zen.compat.OldConfig
import meowing.zen.config.ZenConfig
import meowing.zen.config.ui.ConfigUI
import meowing.zen.events.*
import meowing.zen.feats.Debug
import meowing.zen.feats.Feature
import meowing.zen.feats.FeatureLoader
import meowing.zen.utils.ChatUtils
import meowing.zen.utils.DataUtils
import meowing.zen.utils.TickUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.event.ClickEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent

@Mod(modid = "zen", name = "Zen", version = "1.8.9", useMetadata = true, clientSideOnly = true)
class Zen {
    data class PersistentData (val isFirstInstall: Boolean = true)
    private var eventCall: EventBus.EventCall? = null
    private lateinit var FirstInstall: DataUtils<PersistentData>

    @Target(AnnotationTarget.CLASS)
    annotation class Module

    @Target(AnnotationTarget.CLASS)
    annotation class Command

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        EventBus.post(GameEvent.Load())

        OldConfig.convertConfig(mc.mcDataDir)
        configUI = ZenConfig()
        FeatureLoader.init()
        initializeFeatures()
        executePending()

        FirstInstall = DataUtils("zen-data", PersistentData())

        eventCall = EventBus.register<EntityEvent.Join> ({ event ->
            if (event.entity == mc.thePlayer) {
                ChatUtils.addMessage(
                    "$prefix §fMod loaded.",
                    "§c${FeatureLoader.getModuleCount()} modules §8- §c${FeatureLoader.getLoadtime()}ms §8- §c${FeatureLoader.getCommandCount()} commands"
                )
                val data = FirstInstall.getData()
                if (data.isFirstInstall) {
                    ChatUtils.addMessage("$prefix §fThanks for installing Zen!")
                    ChatUtils.addMessage("§7> §fUse §c/zen §fto open the config or §c/zenhud §fto edit HUD elements")
                    ChatUtils.addMessage("§7> §cDiscord:§b [Discord]", "Discord server", ClickEvent.Action.OPEN_URL, "https://discord.gg/KPmHQUC97G")
                    FirstInstall.setData(data.copy(isFirstInstall = false))
                    FirstInstall.save()
                }
                if (Debug.debugmode) ChatUtils.addMessage("$prefix §fYou have debug mode enabled, restart the game if this was not intentional.")
                UpdateChecker.checkForUpdates()
                eventCall?.unregister()
                eventCall = null
            }
        })

        EventBus.register<GuiEvent.Open> ({ event ->
            if (event.screen is GuiInventory) isInInventory = true
        })

        EventBus.register<GuiEvent.Close> ({
            isInInventory = false
        })

        EventBus.register<AreaEvent.Main> ({
            TickUtils.scheduleServer(1) {
                areaFeatures.forEach { it.update() }
            }
        })

        EventBus.register<AreaEvent.Sub> ({
            TickUtils.scheduleServer(1) {
                subareaFeatures.forEach { it.update() }
            }
        })
    }

    @Mod.EventHandler
    fun stop(event: FMLServerStoppingEvent) {
        EventBus.post(GameEvent.Unload())
    }

    companion object {
        private val pendingCallbacks = mutableListOf<Pair<String, (Any) -> Unit>>()
        private val pendingFeatures = mutableListOf<Feature>()
        private val areaFeatures = mutableListOf<Feature>()
        private val subareaFeatures = mutableListOf<Feature>()
        lateinit var configUI: ConfigUI
        const val prefix = "§7[§bZen§7]"
        val features = mutableListOf<Feature>()
        val mc: Minecraft = Minecraft.getMinecraft()
        var isInInventory = false

        private fun executePending() {
            pendingCallbacks.forEach { (configKey, callback) ->
                configUI.registerListener(configKey, callback)
            }
            pendingCallbacks.clear()
        }

        fun registerListener(configKey: String, instance: Any) {
            val callback: (Any) -> Unit = { _ ->
                if (instance is Feature) instance.update()
            }

            if (::configUI.isInitialized) configUI.registerListener(configKey, callback) else pendingCallbacks.add(configKey to callback)
        }

        fun initializeFeatures() {
            pendingFeatures.forEach { feature ->
                features.add(feature)
                if (feature.hasAreas()) areaFeatures.add(feature)
                if (feature.hasSubareas()) subareaFeatures.add(feature)
                feature.addConfig(configUI)
                feature.initialize()
                feature.configKey?.let {
                    registerListener(it, feature)
                }
                feature.update()
            }
            pendingFeatures.clear()
        }

        fun addFeature(feature: Feature) = pendingFeatures.add(feature)
        fun openConfig() = mc.displayGuiScreen(configUI)
    }
}