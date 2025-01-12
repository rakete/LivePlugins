import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import liveplugin.registerAction
import liveplugin.show
import java.awt.datatransfer.StringSelection

class CutLineAction : AnAction(), DumbAware {
    private val actionManager = ActionManager.getInstance()

    fun copyLine(event: AnActionEvent) {
        // Get the editor and document
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document

        // Get the caret's current line
        val caretModel = editor.caretModel
        val currentLineNumber = caretModel.logicalPosition.line

        // Check if the line number is valid
        if (currentLineNumber < 0 || currentLineNumber >= document.lineCount) return

        // Get the line's start and end offsets
        val lineStartOffset = document.getLineStartOffset(currentLineNumber)
        val lineEndOffset = document.getLineEndOffset(currentLineNumber)

        // Extract the line content
        val lineContent = document.text.substring(lineStartOffset, lineEndOffset)

        // Copy the line content to the clipboard
        val copyPasteManager = CopyPasteManager.getInstance()
        copyPasteManager.setContents(StringSelection(lineContent))
    }

    fun performAction(event: AnActionEvent, actionId: String) {
        val dataContext = event.dataContext
        val inputEvent = event.inputEvent

        val action = actionManager.getAction(actionId)
        if (action != null) {
            val actionEvent = AnActionEvent.createEvent(
                action,
                dataContext,
                null,
                event.place,
                ActionUiKind.NONE,
                inputEvent
            )
            action.actionPerformed(actionEvent)
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        copyLine(event)
        performAction(event, "EditorDeleteLine")
    }
}

// Register the CutLine action and bind it to a specific shortcut
registerAction(id = "CutLine", keyStroke = "ctrl D", action = CutLineAction())
if (!isIdeStartup) show("Enabled CutLine")