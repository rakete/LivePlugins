import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.Editor
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

    /*fun outsideParens(editor: Editor): Boolean {
        val caretModel = editor.caretModel
        val charBefore = caretModel.currentCaret.offset - 1
        if (charBefore < 1) {
            return true
        }
        
        val isNotAfterOpenParen = editor.document.charsSequence[charBefore] != '(' && editor.document.charsSequence[charBefore] != '[' && editor.document.charsSequence[charBefore] != '{'
        val isNotBeforeCloseParen = editor.document.charsSequence[charBefore] != ')' && editor.document.charsSequence[charBefore] != ']' && editor.document.charsSequence[charBefore] != '}'

        val isNotAfterComma = editor.document.charsSequence[charBefore] != ',' && (editor.document.charsSequence[charBefore - 1] != ',' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterPeriod = editor.document.charsSequence[charBefore] != '.' && (editor.document.charsSequence[charBefore - 1] != '.' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterEqual = editor.document.charsSequence[charBefore] != '=' && (editor.document.charsSequence[charBefore - 1] != '=' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterAmpersand = editor.document.charsSequence[charBefore] != '&' && (editor.document.charsSequence[charBefore - 1] != '&' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterPipe = editor.document.charsSequence[charBefore] != '|' && (editor.document.charsSequence[charBefore - 1] != '|' && editor.document.charsSequence[charBefore] == ' ')

        val isNotAfterSemicolon = editor.document.charsSequence[charBefore] != ';' && (editor.document.charsSequence[charBefore - 1] != ';' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterQuestionMark = editor.document.charsSequence[charBefore] != '?' && (editor.document.charsSequence[charBefore - 1] != '?' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterExclamationMark = editor.document.charsSequence[charBefore] != '!' && (editor.document.charsSequence[charBefore - 1] != '!' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterAt = editor.document.charsSequence[charBefore] != '@' && (editor.document.charsSequence[charBefore - 1] != '@' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterDollar = editor.document.charsSequence[charBefore] != '$' && (editor.document.charsSequence[charBefore - 1] != '$' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterBacktick = editor.document.charsSequence[charBefore] != '`' && (editor.document.charsSequence[charBefore - 1] != '`' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterTilde = editor.document.charsSequence[charBefore] != '~' && (editor.document.charsSequence[charBefore - 1] != '~' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterPercent = editor.document.charsSequence[charBefore] != '%' && (editor.document.charsSequence[charBefore - 1] != '%' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterSingleQuote = editor.document.charsSequence[charBefore] != '\'' && (editor.document.charsSequence[charBefore - 1] != '\'' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterDoubleQuote = editor.document.charsSequence[charBefore] != '"' && (editor.document.charsSequence[charBefore - 1] != '"' && editor.document.charsSequence[charBefore] == ' ')

        val isNotAfterPlus = editor.document.charsSequence[charBefore] != '+' && (editor.document.charsSequence[charBefore - 1] != '+' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterMinus = editor.document.charsSequence[charBefore] != '-' && (editor.document.charsSequence[charBefore - 1] != '-' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterAsterisk = editor.document.charsSequence[charBefore] != '*' && (editor.document.charsSequence[charBefore - 1] != '*' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterSlash = editor.document.charsSequence[charBefore] != '/' && (editor.document.charsSequence[charBefore - 1] != '/' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterLessThan = editor.document.charsSequence[charBefore] != '<' && (editor.document.charsSequence[charBefore - 1] != '<' && editor.document.charsSequence[charBefore] == ' ')
        val isNotAfterGreaterThan = editor.document.charsSequence[charBefore] != '>' && (editor.document.charsSequence[charBefore - 1] != '>' && editor.document.charsSequence[charBefore] == ' ')

        return isNotAfterOpenParen && isNotBeforeCloseParen &&
                isNotAfterComma && isNotAfterPeriod && isNotAfterEqual && isNotAfterAmpersand && isNotAfterPipe &&
                isNotAfterSemicolon && isNotAfterQuestionMark && isNotAfterExclamationMark && isNotAfterAt && isNotAfterDollar && isNotAfterBacktick && isNotAfterTilde && isNotAfterPercent && isNotAfterSingleQuote && isNotAfterDoubleQuote &&
                isNotAfterPlus && isNotAfterMinus && isNotAfterAsterisk && isNotAfterSlash && isNotAfterLessThan && isNotAfterGreaterThan
    }*/

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        val clipboardContent = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        val n = clipboardContent?.count { it == '\n' } ?: 0
        if (n == 0 && isBeforeCaretEmpty(editor)) {
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