import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import liveplugin.registerAction
import liveplugin.show
import java.awt.datatransfer.DataFlavor
import java.util.*

class YankIndentAction : AnAction(), DumbAware {
    private val actionManager = ActionManager.getInstance()

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

    fun isBeforeCaretEmpty(editor: Editor): Boolean {
        val caretModel = editor.caretModel
        val currentLine = caretModel.logicalPosition.line

        val document = editor.document
        if (currentLine < 0 || currentLine >= document.lineCount) {
            return false
        }

        val lineStartOffset = document.getLineStartOffset(currentLine)
        val offset = caretModel.offset
        val lineText = document.charsSequence.substring(lineStartOffset, offset)

        return lineText.trim().isEmpty()
    }

    fun isAfterCaretEmpty(editor: Editor): Boolean {
        val caretModel = editor.caretModel
        val currentLine = caretModel.logicalPosition.line

        val document = editor.document
        if (currentLine < 0 || currentLine >= document.lineCount) {
            return false
        }

        val lineEndOffset = document.getLineEndOffset(currentLine)
        val offset = caretModel.offset
        val lineText = document.charsSequence.substring(offset, lineEndOffset)

        return lineText.trim().isEmpty()
    }

    /*fun isNextCharWhitespace(editor: Editor): Boolean {
        val caretModel = editor.caretModel
        val charNext = caretModel.currentCaret.offset + 1

        val document = editor.document
        if (charNext >= editor.document.textLength || charNext >= document.getLineEndOffset(caretModel.logicalPosition.line) || editor.document.charsSequence[charNext] == '\n') {
            return false
        }

        val isWhitespace = editor.document.charsSequence[charNext] == ' ' || editor.document.charsSequence[charNext] == '\t'
        return isWhitespace
    }*/

    /*fun isEntireLineEmpty(editor: Editor): Boolean {
        val caretModel = editor.caretModel
        val currentLine = caretModel.logicalPosition.line

        val document = editor.document
        if (currentLine < 0 || currentLine >= document.lineCount) {
            return false
        }

        val lineStartOffset = document.getLineStartOffset(currentLine)
        val offset = caretModel.offset
        if (lineStartOffset == offset) {
            return false
        }

        val lineEndOffset = document.getLineEndOffset(currentLine)
        val lineText = document.charsSequence.substring(lineStartOffset, lineEndOffset)
        return lineText.trim().isEmpty()
    }*/

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        val clipboardContent = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        val n = clipboardContent?.count { it == '\n' } ?: 0

        if (n == 0 && isBeforeCaretEmpty(editor)) {
            if (isAfterCaretEmpty(editor)) {
                performAction(event, "EditorDeleteLine")

                performAction(event, "EditorUp")
                performAction(event, "EditorLineEnd")
                performAction(event, "EditorEnter")
            }
        }

        val a = editor.caretModel.offset
        performAction(event, "\$Paste")
        val b = editor.caretModel.offset

        val selectionModel = editor.selectionModel
        selectionModel.setSelection(a, b)

        performAction(event, "EmacsStyleIndent")
        Timer().schedule(object : TimerTask() {
            override fun run() {
                // Make sure removal runs on the UI thread
                ApplicationManager.getApplication().invokeLater {
                    selectionModel.removeSelection()
                }
            }
        }, 50)
    }
}

// Register the YankIndent action and bind it to a specific shortcut
registerAction(id = "YankIndent", keyStroke = "ctrl Y", action = YankIndentAction())
if (!isIdeStartup) show("Enabled YankIndent")