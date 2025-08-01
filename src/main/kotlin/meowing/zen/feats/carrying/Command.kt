package meowing.zen.feats.carrying

import meowing.zen.Zen
import meowing.zen.Zen.Companion.prefix
import meowing.zen.config.ConfigDelegate
import meowing.zen.utils.ChatUtils
import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.event.ClickEvent
import net.minecraft.util.BlockPos
import net.minecraft.util.ChatComponentText

@Zen.Command
object carrycommand : CommandBase() {
    private val carrycounter by ConfigDelegate<Boolean>("carrycounter")
    private var currentLogPage = 1

    override fun getCommandName() = "carry"
    override fun getCommandUsage(sender: ICommandSender?) = "/carry <add|remove|list|clear|settotal|setcount|log> [args] - Carry management"
    override fun getRequiredPermissionLevel() = 0
    override fun getCommandAliases() = listOf("zencarry")

    override fun addTabCompletionOptions(sender: ICommandSender?, args: Array<out String?>, pos: BlockPos?): List<String> {
        return when (args.size) {
            1 -> {
                val commands = listOf("add", "remove", "list", "clear", "settotal", "setcount", "log")
                getListOfStringsMatchingLastWord(args, commands)
            }
            2 -> {
                when (args[0]?.lowercase()) {
                    "add", "remove", "rem", "settotal", "setcount" -> {
                        val suggestions = mutableListOf<String>()
                        val onlinePlayers = getAllPlayers()
                        suggestions.addAll(onlinePlayers)
                        if (args[0]?.lowercase() in listOf("remove", "rem", "settotal", "setcount")) {
                            val carryeeNames = CarryCounter.carryees.map { it.name }
                            suggestions.addAll(carryeeNames)
                        }
                        val uniqueSuggestions = suggestions.distinct()
                        getListOfStringsMatchingLastWord(args, uniqueSuggestions)
                    }
                    "log" -> {
                        val logs = CarryCounter.dataUtils.getData().completedCarries
                        val totalPages = (logs.size + 9) / 10
                        val pageNumbers = (1..totalPages).map { it.toString() }
                        getListOfStringsMatchingLastWord(args, pageNumbers)
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                when (args[0]?.lowercase()) {
                    "add", "settotal" -> {
                        val suggestions = listOf("1", "5", "10", "15", "20", "25", "30")
                        getListOfStringsMatchingLastWord(args, suggestions)
                    }
                    "setcount" -> {
                        val playerName = args[1] ?: ""
                        val carryee = CarryCounter.findCarryee(playerName)
                        if (carryee != null) {
                            val suggestions = (0..carryee.total).map { it.toString() }
                            getListOfStringsMatchingLastWord(args, suggestions)
                        } else {
                            val suggestions = listOf("0", "1", "5", "10")
                            getListOfStringsMatchingLastWord(args, suggestions)
                        }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun getAllPlayers(): List<String> {
        val world = Minecraft.getMinecraft().theWorld ?: return emptyList()
        return world.playerEntities
            .filterIsInstance<EntityPlayer>()
            .filter { it.uniqueID?.version() == 4 && it.name.isNotBlank() }
            .map { it.name }
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String?>) {
        if (!carrycounter)
            return ChatUtils.addMessage(
                "$prefix §fPlease enable carry counter first!",
                "§cClick to open settings GUI",
                ClickEvent.Action.RUN_COMMAND,
                "/zen"
            )

        if (sender !is EntityPlayer || args.isEmpty()) return showHelp()

        when (args[0]?.lowercase()) {
            "add" -> if (args.size >= 3) addCarryee(args[1] ?: "", args[2]?.toIntOrNull() ?: 0) else showUsage("add <player> <count>")
            "remove", "rem" -> if (args.size >= 2) removeCarryee(args[1] ?: "") else showUsage("remove <player>")
            "settotal" -> if (args.size >= 3) setTotal(args[1] ?: "", args[2]?.toIntOrNull() ?: 0) else showUsage("settotal <player> <total>")
            "setcount" -> if (args.size >= 3) setCount(args[1] ?: "", args[2]?.toIntOrNull() ?: 0) else showUsage("setcount <player> <count>")
            "list", "ls" -> listCarryees()
            "clear" -> clearCarryees()
            "log", "logs" -> showLogs(args.getOrNull(1)?.toIntOrNull() ?: currentLogPage)
            "devtest" -> CarryCounter.carryees.forEach { it.onDeath() }
            else -> showHelp()
        }
    }

    private fun addCarryee(playerName: String, count: Int) {
        if (playerName.isBlank()) return ChatUtils.addMessage("$prefix §fPlayer name cannot be empty!")
        if (count <= 0) return ChatUtils.addMessage("$prefix §fInvalid count! Must be positive.")

        val carryee = CarryCounter.addCarryee(playerName, count)
        if (carryee != null) {
            if (carryee.total == count) ChatUtils.addMessage("$prefix §fAdded §b$playerName§f for §b$count§f carries.")
            else ChatUtils.addMessage("$prefix §fUpdated §b$playerName§f to §b${carryee.total}§f total (§b${carryee.count}§f/§b${carryee.total}§f)")
        }
    }

    private fun removeCarryee(playerName: String) {
        if (playerName.isBlank()) return ChatUtils.addMessage("$prefix §fPlayer name cannot be empty!")
        val removed = CarryCounter.removeCarryee(playerName)
        ChatUtils.addMessage("$prefix §f${if (removed) "Removed" else "Player not found:"} §b$playerName")
    }

    private fun setTotal(playerName: String, total: Int) {
        if (playerName.isBlank()) return ChatUtils.addMessage("$prefix §fPlayer name cannot be empty!")
        if (total <= 0) return ChatUtils.addMessage("$prefix §fTotal must be positive.")

        val carryee = CarryCounter.findCarryee(playerName)
        if (carryee != null) {
            carryee.total = total
            ChatUtils.addMessage("$prefix §fSet §b$playerName§f total to §b$total§f (§b${carryee.count}§f/§b$total§f)")
        } else ChatUtils.addMessage("$prefix §fPlayer §b$playerName§f not found!")
    }

    private fun setCount(playerName: String, count: Int) {
        if (playerName.isBlank()) return ChatUtils.addMessage("$prefix §fPlayer name cannot be empty!")
        if (count < 0) return ChatUtils.addMessage("$prefix §fCount cannot be negative.")

        val carryee = CarryCounter.findCarryee(playerName)
        if (carryee != null) {
            carryee.count = count
            ChatUtils.addMessage("$prefix §fSet §b$playerName§f count to §b$count§f (§b$count§f/§b${carryee.total}§f)")
            if (count >= carryee.total) carryee.complete()
        } else ChatUtils.addMessage("$prefix §fPlayer §b$playerName§f not found!")
    }

    private fun clearCarryees() {
        val count = CarryCounter.carryees.size
        CarryCounter.clearCarryees()
        ChatUtils.addMessage("$prefix §fCleared §b$count§f carries.")
    }

    private fun listCarryees() {
        if (CarryCounter.carryees.isEmpty()) return ChatUtils.addMessage("$prefix §fNo active carries.")

        ChatUtils.addMessage("$prefix §fActive Carries:")
        CarryCounter.carryees.forEach { carryee ->
            val progress = "§b${carryee.count}§f/§b${carryee.total}"
            val lastBoss = if (carryee.count > 0) "§7(${carryee.getTimeSinceLastBoss()} ago)" else ""
            ChatUtils.addMessage("§7> §b${carryee.name}§f - $progress $lastBoss")
        }
    }

    private fun showLogs(page: Int = currentLogPage) {
        val logs = CarryCounter.dataUtils.getData().completedCarries.sortedByDescending { it.timestamp }
        if (logs.isEmpty()) return ChatUtils.addMessage("$prefix §fNo carry logs found.")

        val totalCarries = logs.sumOf { it.totalCarries }
        val totalPages = (logs.size + 9) / 10
        currentLogPage = page.coerceIn(1, totalPages)

        val startIndex = (currentLogPage - 1) * 10
        val endIndex = (startIndex + 10).coerceAtMost(logs.size)

        ChatUtils.addMessage("§7⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤")

        val prevPage = if (currentLogPage > 1) "§b[<]" else "§7[<]"
        val nextPage = if (currentLogPage < totalPages) "§b[>]" else "§7[>]"

        val player = Minecraft.getMinecraft().thePlayer ?: return
        player.addChatMessage(
            ChatComponentText("$prefix §fCarry Logs - §fPage §b$currentLogPage§f/§b$totalPages ")
                .appendSibling(
                    ChatComponentText(prevPage).apply {
                        chatStyle = ChatUtils.createChatStyle(
                            prevPage,
                            ClickEvent.Action.RUN_COMMAND,
                            if (currentLogPage > 1) "/carry log ${currentLogPage - 1}" else ""
                        )
                    }
                )
                .appendSibling(ChatComponentText(" §7| "))
                .appendSibling(
                    ChatComponentText(nextPage).apply {
                        chatStyle = ChatUtils.createChatStyle(
                            nextPage,
                            ClickEvent.Action.RUN_COMMAND,
                            if (currentLogPage < totalPages) "/carry log ${currentLogPage + 1}" else ""
                        )
                    }
                )
        )

        logs.subList(startIndex, endIndex).forEach { log ->
            val date = java.text.SimpleDateFormat("d/M/yyyy").format(java.util.Date(log.timestamp))
            val time = java.text.SimpleDateFormat("HH:mm").format(java.util.Date(log.timestamp))
            ChatUtils.addMessage("§7> §b${log.playerName} §7- §c$date §fat §c$time §7- §b${log.totalCarries} §fcarries")
        }

        ChatUtils.addMessage("§c§l| §fTotal carries: §b$totalCarries")
        ChatUtils.addMessage("§7⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤")
    }

    private fun showUsage(command: String) = ChatUtils.addMessage("$prefix §fUsage: §c/carry $command")

    private fun showHelp() {
        ChatUtils.addMessage("$prefix §fCarry Commands:")
        listOf(
            "add §c<player> <count>§7 - §fAdd carries",
            "settotal §c<player> <total>§7 - §fSet total carries",
            "setcount §c<player> <count>§7 - §fSet current count",
            "remove §c<player>§7 - §fRemove player",
            "log §c[page]§7 - §fShow carry history",
            "list§7 - §fShow active carries",
            "clear§7 - §fClear all carries"
        ).forEach { ChatUtils.addMessage("§7> §7/§bcarry $it") }
    }
}