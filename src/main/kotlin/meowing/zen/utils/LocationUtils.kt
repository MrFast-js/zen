package meowing.zen.utils

import meowing.zen.events.AreaEvent
import meowing.zen.events.EventBus
import meowing.zen.events.PacketEvent
import meowing.zen.events.WorldEvent
import meowing.zen.utils.Utils.removeEmotes
import meowing.zen.utils.Utils.removeFormatting
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S3BPacketScoreboardObjective
import net.minecraft.network.play.server.S3EPacketTeams

object LocationUtils {
    private val areaRegex = "^(?:Area|Dungeon): ([\\w ]+)$".toRegex()
    private val subAreaRegex = "^ ([⏣ф]) .*".toRegex()
    private val uselessRegex = "^[⏣ф] ".toRegex()
    private val lock = Any()
    private var cachedAreas = mutableMapOf<String?, Boolean>()
    private var cachedSubareas = mutableMapOf<String?, Boolean>()
    var area: String? = null
        private set
    var subarea: String? = null
        private set

    init {
        EventBus.register<PacketEvent.Received> ({ event ->
            when (val packet = event.packet) {
                is S38PacketPlayerListItem -> {
                    if (packet.action != S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME && packet.action != S38PacketPlayerListItem.Action.ADD_PLAYER) return@register
                    packet.entries?.forEach { entry ->
                        val displayName = entry.displayName?.unformattedText ?: return@forEach
                        val line = displayName.removeEmotes()
                        val match = areaRegex.find(line) ?: return@forEach
                        val newArea = match.groupValues[1]
                        if (newArea != area) {
                            synchronized(lock) {
                                EventBus.post(AreaEvent.Main(newArea))
                                area = newArea.lowercase()
                            }
                        }
                    }
                }
                is S3EPacketTeams -> {
                    val teamPrefix = packet.prefix
                    val teamSuffix = packet.suffix
                    if (teamPrefix.isEmpty() || teamSuffix.isEmpty()) return@register

                    val line = teamPrefix + teamSuffix
                    if (!subAreaRegex.matches(line.removeFormatting())) return@register
                    if (line.endsWith("cth") || line.endsWith("ch")) return@register
                    val cleanSubarea = line.removeFormatting().replace(uselessRegex, "").trim().lowercase()
                    if (cleanSubarea != subarea) {
                        synchronized(lock) {
                            EventBus.post(AreaEvent.Sub(cleanSubarea))
                            subarea = cleanSubarea
                        }
                    }
                }
            }
        })

        EventBus.register<AreaEvent.Main> ({
            synchronized(lock) {
                cachedAreas.clear()
            }
        })

        EventBus.register<AreaEvent.Sub> ({
            synchronized(lock) {
                cachedSubareas.clear()
            }
        })
    }

    fun checkArea(areaLower: String?): Boolean {
        return synchronized(lock) {
            cachedAreas.getOrPut(areaLower) {
                areaLower?.let { area == it } ?: true
            }
        }
    }

    fun checkSubarea(subareaLower: String?): Boolean {
        return synchronized(lock) {
            cachedSubareas.getOrPut(subareaLower) {
                subareaLower?.let { subarea?.contains(it) == true } ?: true
            }
        }
    }
}