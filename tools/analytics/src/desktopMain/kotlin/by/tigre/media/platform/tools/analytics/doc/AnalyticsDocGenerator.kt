package by.tigre.media.platform.tools.analytics.doc

import java.io.File
import kotlin.io.walkTopDown

fun main(args: Array<String>) {
    val analyticsRoot = File(args[0])
    val outputFile = File(args[1])
    val events = AnalyticsDocParser.parse(analyticsRoot)
    outputFile.parentFile?.mkdirs()
    outputFile.writeText(AnalyticsDocRenderer.render(events))
    println("Wrote ${events.size} analytics events to ${outputFile.absolutePath}")
}

private object AnalyticsDocParser {
    private val scopeRegex = Regex("""@AnalyticsScope\(([^)]*)\)""")
    private val docRegex = Regex("""@AnalyticsDoc\("([^"]*)"\)""")
    private val eventRegex = Regex("""(?:data\s+)?(?:class|object)\s+(\w+)""")
    private val nameRegex = Regex("""Action\("([^"]+)"\)|Screen\("([^"]+)"(?:,\s*skip\s*=\s*true)?\)|: Screen\("([^"]+)"(?:,\s*skip\s*=\s*true)?\)""")

    fun parse(analyticsRoot: File): List<AnalyticsEventDoc> = analyticsRoot
        .walkTopDown()
        .filter { file ->
            file.extension == "kt" &&
                file.path.contains("""${File.separator}src${File.separator}commonMain${File.separator}""")
        }
        .flatMap { file -> parseFile(file, moduleName(file)) }
        .sortedWith(compareBy({ it.module }, { it.kind }, { it.name }))
        .toList()

    private fun moduleName(file: File): String = when {
        file.path.contains("""${File.separator}analytics${File.separator}music${File.separator}""") -> "music"
        file.path.contains("""${File.separator}analytics${File.separator}book${File.separator}""") -> "book"
        else -> "common"
    }

    private fun parseFile(file: File, module: String): List<AnalyticsEventDoc> {
        val lines = file.readLines()
        val events = mutableListOf<AnalyticsEventDoc>()
        var pendingScopes = emptyList<String>()
        var pendingDoc = ""

        lines.forEachIndexed { index, line ->
            scopeRegex.find(line)?.let { match ->
                pendingScopes = match.groupValues[1]
                    .split(',')
                    .map { it.trim().removePrefix("AnalyticsApp.") }
                    .filter { it.isNotEmpty() }
            }
            docRegex.find(line)?.let { match ->
                pendingDoc = match.groupValues[1]
            }

            val isEventLine = line.contains(" : Action(") || line.contains(" : Screen(") ||
                line.contains(") : Action(") || line.contains(") : Screen(") ||
                (line.contains("data object ") && line.contains(" : Action")) ||
                (line.contains("data object ") && line.contains(" : Screen")) ||
                (line.contains("data class ") && line.contains(" : Screen"))

            if (isEventLine) {
                val eventId = eventRegex.find(line)?.groupValues?.get(1)
                    ?: lines.subList((index - 4).coerceAtLeast(0), index)
                        .asReversed()
                        .firstNotNullOfOrNull { eventRegex.find(it)?.groupValues?.get(1) }
                    .orEmpty()
                val eventName = nameRegex.find(line)?.destructured?.let { (a, b, c) ->
                    listOf(a, b, c).firstOrNull { it.isNotEmpty() }
                }.orEmpty()
                val kind = if (line.contains("Action(")) "action" else "screen"
                if (eventName.isNotEmpty()) {
                    events += AnalyticsEventDoc(
                        module = module,
                        kind = kind,
                        id = eventId,
                        name = eventName,
                        apps = pendingScopes,
                        description = pendingDoc,
                        skip = line.contains("skip = true"),
                    )
                }
                pendingScopes = emptyList()
                pendingDoc = ""
            }
        }
        return events
    }
}

private object AnalyticsDocRenderer {
    fun render(events: List<AnalyticsEventDoc>): String = buildString {
        appendLine("# Analytics events")
        appendLine()
        appendLine("Auto-generated from `@AnalyticsScope` annotations. Regenerate:")
        appendLine()
        appendLine("```bash")
        appendLine("./gradlew :tools:analytics:generateAnalyticsDocs")
        appendLine("```")
        appendLine()
        appendLine("| Module | Kind | Event name | ID | Player | AudioBook | Desktop | Skip | Description |")
        appendLine("|--------|------|------------|-----|:------:|:---------:|:-------:|:----:|-------------|")
        events.forEach { event ->
            appendLine(
                "| ${event.module} | ${event.kind} | `${event.name}` | ${event.id} | " +
                    "${mark(event, "PLAYER")} | ${mark(event, "AUDIOBOOK")} | ${mark(event, "DESKTOP")} | " +
                    "${if (event.skip) "yes" else ""} | ${event.description} |"
            )
        }
    }

    private fun mark(event: AnalyticsEventDoc, app: String): String =
        if (event.apps.contains(app)) "✓" else ""
}

private data class AnalyticsEventDoc(
    val module: String,
    val kind: String,
    val id: String,
    val name: String,
    val apps: List<String>,
    val description: String,
    val skip: Boolean,
)
