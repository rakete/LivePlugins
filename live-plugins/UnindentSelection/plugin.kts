import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import liveplugin.registerAction
import liveplugin.show

class UnindentSelectionAction : AnAction(), DumbAware {
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

    fun unindentSelection(event: AnActionEvent) {
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
            
            performAction(event, "EditorUnindentSelection")

            val newStartOffset = if (wasAtLineStart) {
                document.getLineStartOffset(startLine)
            } else {
                val lineStartOffset = document.getLineStartOffset(startLine)
                val lineEndOffset = document.getLineEndOffset(startLine)
                val lineText = document.charsSequence.subSequence(lineStartOffset, lineEndOffset)

                // Find the start of the word at the original selection start
                val offsetInLine = selectionStart - startOffset
                var wordStart = offsetInLine

                // Move back to find the beginning of the word
                while (wordStart > 0 && !lineText[wordStart - 1].isWhitespace()) {
                    wordStart--
                }

                (lineStartOffset + wordStart).coerceAtLeast(lineStartOffset)
            }
            val newEndOffset = document.getLineEndOffset(endLine).coerceAtMost(document.textLength)

            caret?.setSelection(newStartOffset, newEndOffset)
        } else {
            performAction(event, "EditorUnindentSelection")

            if (selectionStart != null && selectionEnd != null) {
                caret?.setSelection(selectionStart, selectionEnd)
            }
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val caret = editor!!.caretModel.primaryCaret

        if (caret.hasSelection()) {
            unindentSelection(event)
        }
    }
}

registerAction(id = "UnindentSelection", keyStroke = "alt shift BACK_SPACE", action = UnindentSelectionAction())
if (!isIdeStartup) show("Enabled UnindentSelection")