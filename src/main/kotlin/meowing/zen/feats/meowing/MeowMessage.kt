package meowing.zen.feats.meowing

import meowing.zen.Zen
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.ChatEvent
import meowing.zen.feats.Feature
import meowing.zen.utils.ChatUtils
import kotlin.random.Random

@Zen.Module
object MeowMessage : Feature("meowmessage") {
    private val variants = listOf("meow", "mew", "mrow", "nyaa", "purr", "mrrp", "meoww", "nya")

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Meowing", "Cat Speak", ConfigElement(
                "meowmessage",
                "Cat Speak",
                ElementType.Switch(false)
            ), true)
    }

    override fun initialize() {
        register<ChatEvent.Send> { event ->
            if (variants.any { it in event.message }) return@register

            val parts = event.message.split(" ", limit = 2)
            val shouldTransform = if (parts[0].startsWith("/")) {
                parts[0].drop(1) in setOf("gc", "pc", "ac", "msg", "tell", "r", "say", "w", "reply") && parts.size > 1
            } else true

            if (shouldTransform) {
                event.cancel()
                val message = if (parts.size > 1) "${parts[0]} ${transform(parts[1])}" else transform(event.message)
                ChatUtils.chat(message)
            }
        }
    }

    private fun transform(message: String): String {
        val words = message.split(" ")
        val result = mutableListOf<String>()
        for (word in words) {
            result.add(word)
            if (!word.startsWith("/") && Random.nextBoolean()) {
                result.add(variants.random())
            }
        }
        return result.joinToString(" ")
    }
}