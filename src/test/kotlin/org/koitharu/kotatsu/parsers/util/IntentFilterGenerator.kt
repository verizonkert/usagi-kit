package tsuki.util

import org.junit.jupiter.api.Test
import tsuki.MangaLoaderContextMock
import tsuki.TestMangaSource
import java.io.File

class IntentFilterGenerator {

    @Test
    fun generateIntentFilter() {
        val output = File("build/reports/intent-filter.xml")
        output.parentFile?.mkdirs()
        output.printWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("<intent-filter android:autoVerify=\"false\">")
            writer.appendTab().appendLine("<action android:name=\"android.intent.action.VIEW\" />")
            writer.appendLine()
            writer.appendTab().appendLine("<category android:name=\"android.intent.category.DEFAULT\" />")
            writer.appendTab().appendLine("<category android:name=\"android.intent.category.BROWSABLE\" />")
            writer.appendLine()
            writer.appendTab().appendLine("<data android:scheme=\"http\" />")
            writer.appendTab().appendLine("<data android:scheme=\"https\" />")
            writer.appendLine()
            for (source in TestMangaSource.entries) {
                val parser = MangaLoaderContextMock.newParserInstance(source)
                parser.configKeyDomain.presetValues.forEach { domain ->
                    writer.appendTab().append("<data android:host=\"").append(domain).appendLine("\" />")
                }
            }
            writer.appendLine()
            writer.appendLine("</intent-filter>")
        }
        println("Generated intent filter at: ${output.absolutePath}")
    }

    private fun Appendable.appendTab() = append('\t')
}
