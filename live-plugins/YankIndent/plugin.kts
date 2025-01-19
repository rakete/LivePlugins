﻿import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import liveplugin.registerAction
import liveplugin.show
import java.awt.datatransfer.DataFlavor


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

    /*fun isCurrentLineEmpty(editor: Editor): Boolean {
        val caretModel = editor.caretModel
        val currentLine = caretModel.logicalPosition.line

        val document = editor.document
        if (currentLine < 0 || currentLine >= document.lineCount) {
            return false
        }

        val lineStartOffset = document.getLineStartOffset(currentLine)
        val lineEndOffset = document.getLineEndOffset(currentLine)
        val lineText = document.charsSequence.substring(lineStartOffset, lineEndOffset)

        return lineText.trim().isEmpty()
    }*/

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        val clipboardContent = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        val n = clipboardContent?.count { it == '\n' } ?: 0
        if (n == 0) {
            performAction(event, "EditorUp")
            performAction(event, "EditorLineEnd")
            performAction(event, "EditorEnter")
        }

        val a = editor.caretModel.offset
        performAction(event, "\$Paste")
        val b = editor.caretModel.offset

        val selectionModel = editor.selectionModel
        selectionModel.setSelection(a, b)

        performAction(event, "EmacsStyleIndent")
    }
}

// Register the YankIndent action and bind it to a specific shortcut
registerAction(id = "YankIndent", keyStroke = "ctrl Y", action = YankIndentAction())
if (!isIdeStartup) show("Enabled YankIndent")