import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.codeInsight.inline.completion.InlineCompletionEvent
import com.intellij.codeInsight.inline.completion.InlineCompletionProvider
import com.intellij.codeInsight.inline.completion.InlineCompletionProviderID
import com.intellij.codeInsight.inline.completion.InlineCompletionRequest
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSingleSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableEP
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import liveplugin.registerAction
import liveplugin.show
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------------------
// Settings storage (application-level; token lives in the IDE Password Safe)
// ---------------------------------------------------------------------------
data class FimConfig(
    val baseUrl: String,
    val apiToken: String,
    val model: String,
    val promptTemplate: String,
    val prefixLines: Int,
    val suffixLines: Int,
    val maxTokens: Int,
    val maxSuggestionLines: Int,
    val temperature: Double,
    val topP: Double,
    val stopSequences: List<String>,
    val connectTimeoutMs: Int,
    val readTimeoutMs: Int,
)

object FimSettings {
    private const val P = "customFim."
    private val props get() = PropertiesComponent.getInstance()

    const val DEFAULT_BASE_URL = "https://lmg-rotom.jedi.lazor.de/v1"
    const val DEFAULT_MODEL = "Ditto Tabbyapi/default-model"
    const val DEFAULT_TEMPLATE = "<|fim_prefix|>{prefix}<|fim_suffix|>{suffix}<|fim_middle|>"
    const val DEFAULT_STOP = "<|file_separator|>\n<|endoftext|>\n\\n\\n"
    const val DEFAULT_TEMPERATURE = 0.1
    const val DEFAULT_TOP_P = 0.95

    private fun str(key: String, default: String) = props.getValue(P + key, default)
    private fun setStr(key: String, value: String, default: String) = props.setValue(P + key, value, default)
    private fun int(key: String, default: Int) = props.getInt(P + key, default)
    private fun setInt(key: String, value: Int, default: Int) = props.setValue(P + key, value, default)

    var baseUrl: String
        get() = str("baseUrl", DEFAULT_BASE_URL)
        set(v) = setStr("baseUrl", v.trim(), DEFAULT_BASE_URL)

    var model: String
        get() = str("model", DEFAULT_MODEL)
        set(v) = setStr("model", v.trim(), DEFAULT_MODEL)

    var promptTemplate: String
        get() = str("promptTemplate", DEFAULT_TEMPLATE)
        set(v) = setStr("promptTemplate", v, DEFAULT_TEMPLATE)

    var prefixLines: Int
        get() = int("prefixLines", 60)
        set(v) = setInt("prefixLines", v, 60)

    var suffixLines: Int
        get() = int("suffixLines", 30)
        set(v) = setInt("suffixLines", v, 30)

    var maxTokens: Int
        get() = int("maxTokens", 128)
        set(v) = setInt("maxTokens", v, 128)

    var maxSuggestionLines: Int
        get() = int("maxSuggestionLines", 20)
        set(v) = setInt("maxSuggestionLines", v, 20)

    var temperature: Double
        get() = str("temperature", DEFAULT_TEMPERATURE.toString()).toDoubleOrNull() ?: DEFAULT_TEMPERATURE
        set(v) = setStr("temperature", v.toString(), DEFAULT_TEMPERATURE.toString())

    var topP: Double
        get() = str("topP", DEFAULT_TOP_P.toString()).toDoubleOrNull() ?: DEFAULT_TOP_P
        set(v) = setStr("topP", v.toString(), DEFAULT_TOP_P.toString())

    /** One stop sequence per line; `\n` and `\t` escapes are supported. */
    var stopSequencesRaw: String
        get() = str("stopSequences", DEFAULT_STOP)
        set(v) = setStr("stopSequences", v, DEFAULT_STOP)

    var connectTimeoutMs: Int
        get() = int("connectTimeoutMs", 5000)
        set(v) = setInt("connectTimeoutMs", v, 5000)

    var readTimeoutMs: Int
        get() = int("readTimeoutMs", 10000)
        set(v) = setInt("readTimeoutMs", v, 10000)

    var autoTrigger: Boolean
        get() = props.getBoolean(P + "autoTrigger", false)
        set(v) = props.setValue(P + "autoTrigger", v, false)

    var debounceMs: Int
        get() = int("debounceMs", 400)
        set(v) = setInt("debounceMs", v, 400)

    // Token: stored securely, cached in memory (the settings UI polls getters frequently)
    private val tokenAttributes = CredentialAttributes(generateServiceName("CustomFimCompletion", "apiToken"))
    @Volatile private var cachedToken: String? = null

    var apiToken: String
        get() = cachedToken ?: (PasswordSafe.instance.getPassword(tokenAttributes) ?: "").also { cachedToken = it }
        set(v) {
            cachedToken = v
            PasswordSafe.instance.setPassword(tokenAttributes, v.ifBlank { null })
        }

    fun parseStopSequences(raw: String): List<String> =
        raw.lines()
            .filter { it.isNotEmpty() }
            .map { it.replace("\\n", "\n").replace("\\t", "\t") }

    fun snapshot() = FimConfig(
        baseUrl = baseUrl,
        apiToken = apiToken,
        model = model,
        promptTemplate = promptTemplate,
        prefixLines = prefixLines,
        suffixLines = suffixLines,
        maxTokens = maxTokens,
        maxSuggestionLines = maxSuggestionLines,
        temperature = temperature,
        topP = topP,
        stopSequences = parseStopSequences(stopSequencesRaw),
        connectTimeoutMs = connectTimeoutMs,
        readTimeoutMs = readTimeoutMs,
    )
}

// ---------------------------------------------------------------------------
// Settings page: Settings | Tools | Custom FIM Completion
// ---------------------------------------------------------------------------
class FimSettingsConfigurable : BoundConfigurable("Custom FIM Completion") {
    override fun createPanel(): DialogPanel = panel {
        group("Server") {
            row("Base URL:") {
                textField().bindText(FimSettings::baseUrl).align(AlignX.FILL)
                    .comment("<code>/completions</code> is appended automatically")
            }
            row("API token:") {
                passwordField().bindText(FimSettings::apiToken).align(AlignX.FILL)
                    .comment("Stored in the IDE password safe. Leave empty if not required.")
            }
            row("Model:") {
                textField().bindText(FimSettings::model).align(AlignX.FILL)
            }
            row("Connect timeout (ms):") {
                spinner(500..60_000, 500).bindIntValue(FimSettings::connectTimeoutMs)
            }
            row("Read timeout (ms):") {
                spinner(1_000..300_000, 1_000).bindIntValue(FimSettings::readTimeoutMs)
            }
        }

        group("Context") {
            row("Lines before caret:") {
                spinner(0..10_000, 10).bindIntValue(FimSettings::prefixLines)
            }
            row("Lines after caret:") {
                spinner(0..10_000, 10).bindIntValue(FimSettings::suffixLines)
            }
            row("Prompt template:") {
                textField().bindText(FimSettings::promptTemplate).align(AlignX.FILL)
                    .comment("Use <code>{prefix}</code> and <code>{suffix}</code> placeholders")
            }
        }

        group("Generation") {
            row("Max tokens:") {
                spinner(1..8_192, 8).bindIntValue(FimSettings::maxTokens)
            }
            row("Max suggestion lines:") {
                spinner(1..500, 1).bindIntValue(FimSettings::maxSuggestionLines)
                    .comment("Longer completions are truncated")
            }
            row("Temperature:") {
                textField()
                    .bindText(
                        { FimSettings.temperature.toString() },
                        { text -> text.toDoubleOrNull()?.let { FimSettings.temperature = it } }
                    )
                    .columns(COLUMNS_SHORT)
                    .validationOnInput { field ->
                        val v = field.text.toDoubleOrNull()
                        if (v != null && v in 0.0..2.0) null else error("Enter a number between 0 and 2")
                    }
                    .validationOnApply { field ->
                        val v = field.text.toDoubleOrNull()
                        if (v != null && v in 0.0..2.0) null else error("Enter a number between 0 and 2")
                    }
            }
            row("Top P:") {
                textField()
                    .bindText(
                        { FimSettings.topP.toString() },
                        { text -> text.toDoubleOrNull()?.let { FimSettings.topP = it } }
                    )
                    .columns(COLUMNS_SHORT)
                    .validationOnInput { field ->
                        val v = field.text.toDoubleOrNull()
                        if (v != null && v in 0.0..1.0) null else error("Enter a number between 0 and 1")
                    }
                    .validationOnApply { field ->
                        val v = field.text.toDoubleOrNull()
                        if (v != null && v in 0.0..1.0) null else error("Enter a number between 0 and 1")
                    }
            }
            row("Stop sequences:") {
                textArea().rows(4).bindText(FimSettings::stopSequencesRaw).align(AlignX.FILL)
                    .comment("One per line. Use <code>\\n</code> for newline and <code>\\t</code> for tab.")
            }
        }

        group("Triggering") {
            lateinit var auto: Cell<javax.swing.JCheckBox>
            row {
                auto = checkBox("Suggest automatically while typing").bindSelected(FimSettings::autoTrigger)
            }
            row("Debounce delay (ms):") {
                spinner(0..5_000, 50).bindIntValue(FimSettings::debounceMs)
            }.enabledIf(auto.selected)
            row {
                comment("Shift+Tab requests a suggestion / accepts the next word. Tab accepts the whole suggestion.")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// FIM provider: produces gray-text suggestions instead of inserting directly
// ---------------------------------------------------------------------------
class CustomFimInlineProvider : InlineCompletionProvider {
    private val gson = Gson()
    private val placeholderRegex = Regex("\\{(prefix|suffix)}")

    override val id = InlineCompletionProviderID("CustomFimInlineProvider")

    override fun isEnabled(event: InlineCompletionEvent): Boolean =
        event is InlineCompletionEvent.DirectCall ||
                (FimSettings.autoTrigger && event is InlineCompletionEvent.DocumentChange)

    override suspend fun getSuggestion(request: InlineCompletionRequest): InlineCompletionSuggestion {
        // Debounce automatic requests: the coroutine is cancelled if the user keeps typing
        if (request.event !is InlineCompletionEvent.DirectCall) {
            val debounce = FimSettings.debounceMs
            if (debounce > 0) delay(debounce.toLong())
        }

        val config = withContext(Dispatchers.IO) { FimSettings.snapshot() }
        val (prefix, suffix) = readAction { extractContext(request.document, request.endOffset, config) }

        val completion = withContext(Dispatchers.IO) { fetchCompletion(config, prefix, suffix) }
            ?.lines()?.take(config.maxSuggestionLines)?.joinToString("\n")

        if (completion.isNullOrEmpty()) return InlineCompletionSuggestion.Empty

        return InlineCompletionSingleSuggestion.build {
            emit(InlineCompletionGrayTextElement(completion))
        }
    }

    private fun extractContext(document: Document, caretOffset: Int, config: FimConfig): Pair<String, String> {
        if (document.lineCount == 0) return "" to ""
        val text = document.charsSequence
        val caretLine = document.getLineNumber(caretOffset)
        val prefixStart = document.getLineStartOffset(max(0, caretLine - config.prefixLines))
        val suffixLineEnd = min(document.lineCount - 1, caretLine + config.suffixLines)
        val suffixEnd =
            if (suffixLineEnd >= document.lineCount) text.length else document.getLineEndOffset(suffixLineEnd)
        return text.subSequence(prefixStart, caretOffset).toString() to
                text.subSequence(caretOffset, suffixEnd).toString()
    }

    private fun buildPrompt(template: String, prefix: String, suffix: String): String =
        // Single pass, so placeholder-like text inside the code itself isn't replaced
        placeholderRegex.replace(template) { if (it.groupValues[1] == "prefix") prefix else suffix }

    private fun fetchCompletion(config: FimConfig, prefix: String, suffix: String): String? {
        return try {
            val endpoint = config.baseUrl.trimEnd('/') + "/completions"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = config.connectTimeoutMs
                readTimeout = config.readTimeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (config.apiToken.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${config.apiToken}")
                }
            }

            val stopSequences = config.stopSequences.toMutableList()
            val suffixLines = suffix.lines()
            val firstLine = suffixLines.firstOrNull()
            val secondSuffixLine = if (firstLine?.trim().isNullOrEmpty()) {
                suffixLines.drop(1).firstOrNull()
            } else {
                firstLine
            }
            if (!secondSuffixLine?.trim().isNullOrEmpty()) {
                stopSequences.add("\n" + secondSuffixLine)
            }

            val requestBody = mapOf(
                "model" to config.model,
                "prompt" to buildPrompt(config.promptTemplate, prefix, suffix),
                "max_tokens" to config.maxTokens,
                "temperature" to config.temperature,
                "top_p" to config.topP,
                "stop" to stopSequences
            )

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(gson.toJson(requestBody))
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                show("FIM completion error: HTTP ${connection.responseCode}")
                return null
            }

            val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val choices = gson.fromJson(responseBody, JsonObject::class.java).getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                choices[0].asJsonObject.get("text")?.asString
            } else null
        } catch (e: Exception) {
            show("FIM completion error: ${e.message}")
            null
        }
    }
}

// ---------------------------------------------------------------------------
// Shift+Tab: accept next word if a suggestion is shown, otherwise request one
// ---------------------------------------------------------------------------
class ShiftTabCompleteByWord : AnAction(), DumbAware {
    private val actionManager = ActionManager.getInstance()

    private fun performAction(event: AnActionEvent, name: String) {
        val action = actionManager.getAction(name) ?: return
        val actionEvent = AnActionEvent.createEvent(
            action, event.dataContext, null, event.place, ActionUiKind.NONE, event.inputEvent
        )
        action.actionPerformed(actionEvent)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val caret = editor.caretModel.primaryCaret
        val startColumn = caret.visualPosition.column

        performAction(event, "InsertInlineCompletionWordAction")

        if (caret.visualPosition.column == startColumn) {
            performAction(event, "CallInlineCompletionAction")
        }
    }
}

// ---------------------------------------------------------------------------
// Registration (everything is unregistered when the live plugin is unloaded)
// ---------------------------------------------------------------------------
val fimConfigurableEP = object : ConfigurableEP<Configurable>() {
    init {
        id = "custom.fim.completion.settings"
        displayName = "Shift Tab Complete By Word"
        groupId = "tools"
        PluginManagerCore.getPlugin(PluginId.getId("LivePlugin"))?.let { pluginDescriptor = it }
    }

    override fun canCreateConfigurable() = true
    override fun createConfigurable(): Configurable = FimSettingsConfigurable()
}
ExtensionPointName.create<ConfigurableEP<Configurable>>("com.intellij.applicationConfigurable")
    .point
    .registerExtension(fimConfigurableEP, pluginDisposable)

InlineCompletionProvider.EP_NAME.point.registerExtension(
    CustomFimInlineProvider(),
    LoadingOrder.FIRST,
    pluginDisposable
)

registerAction(id = "ShiftTabCompleteByWord", keyStroke = "shift TAB", action = ShiftTabCompleteByWord())
if (!isIdeStartup) show("Enabled ShiftTabCompleteByWord")