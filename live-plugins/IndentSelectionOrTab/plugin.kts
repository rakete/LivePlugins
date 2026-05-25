import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import liveplugin.registerAction
import liveplugin.show

class IndentSelectionOrTabAction : AnAction(), DumbAware {
    val actionManager = ActionManager.getInstance()

    fun performAction(event: AnActionEvent, name: String) {
        val dataContext = event.dataContext
        val inputEvent = event.inputEvent

        val action = actionManager.getAction(name)
        if (action != null) {
            val actionEvent =
                AnActionEvent.createEvent(action, dataContext, null, event.place, ActionUiKind.NONE, inputEvent)
            action.actionPerformed(actionEvent)
        }
    }

    fun indentSelection(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val caret = editor?.caretModel?.primaryCaret
        val document = editor?.document
        val selectionStart = caret?.selectionStart
        val selectionEnd = caret?.selectionEnd

        if (editor != null && document != null && selectionStart != null && selectionEnd != null) {
            val startLine = document.getLineNumber(selectionStart)
            val endLine = document.getLineNumber(selectionEnd)
            val startOffset = document.getLineStartOffset(startLine)
            val wasAtLineStart = selectionStart == startOffset

            performAction(event, "EditorIndentSelection")

            val newStartOffset = if (wasAtLineStart) {
                document.getLineStartOffset(startLine)
            } else {
                val lineStartOffset = document.getLineStartOffset(startLine)
                val offsetInLine = selectionStart - startOffset
                lineStartOffset + offsetInLine
            }
            val newEndOffset = document.getLineEndOffset(endLine).coerceAtMost(document.textLength)

            caret?.setSelection(newStartOffset, newEndOffset)
        } else {
            performAction(event, "EditorIndentSelection")

            if (selectionStart != null && selectionEnd != null) {
                caret?.setSelection(selectionStart, selectionEnd)
            }
        }
    }

    fun insertTab(event: AnActionEvent) {
        performAction(event, "EditorTab")
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val caret = editor!!.caretModel.primaryCaret

        if (caret.hasSelection()) {
            indentSelection(event)
        } else {
            insertTab(event)
        }
    }
}

registerAction(id = "IndentSelectionOrTab", keyStroke = "ctrl shift TAB", action = IndentSelectionOrTabAction())
if (!isIdeStartup) show("Enabled IndentSelectionOrTab")