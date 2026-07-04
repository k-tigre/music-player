package by.tigre.audiobook.marketing

import android.content.Context
import java.io.File
import java.io.InputStreamReader
import java.util.Properties

object MarketingScreenshotResources {

    private val stringsByLocale = mutableMapOf<MarketingScreenshotLocale, Properties>()

    fun string(locale: MarketingScreenshotLocale, key: String, vararg formatArgs: Any): String {
        val template = strings(locale).getProperty(key)
            ?: error("Missing marketing string: $key")
        return if (formatArgs.isEmpty()) template else String.format(template, *formatArgs)
    }

    fun coverUri(context: Context, locale: MarketingScreenshotLocale, fileName: String): File =
        materializeCover(context, locale, fileName)

    private fun openResource(path: String): java.io.InputStream {
        val loader = MarketingScreenshotResources::class.java.classLoader
        return checkNotNull(loader?.getResourceAsStream(path)) {
            "Missing marketing resource: $path"
        }
    }

    private fun strings(locale: MarketingScreenshotLocale): Properties =
        stringsByLocale.getOrPut(locale) {
            Properties().apply {
                val resourcePath = when (locale) {
                    MarketingScreenshotLocale.En -> "marketing/strings_en.properties"
                    MarketingScreenshotLocale.Ru -> "marketing/strings_ru.properties"
                }
                openResource(resourcePath).use { input ->
                    load(InputStreamReader(input, Charsets.UTF_8))
                }
            }
        }

    private fun materializeCover(
        context: Context,
        locale: MarketingScreenshotLocale,
        fileName: String,
    ): File {
        val cacheFile = File(context.cacheDir, "marketing/${locale.folderName}/$fileName")
        cacheFile.parentFile?.mkdirs()
        val resourcePath = "marketing/covers/${locale.folderName}/$fileName"
        openResource(resourcePath).use { input ->
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        }
        return cacheFile
    }
}
