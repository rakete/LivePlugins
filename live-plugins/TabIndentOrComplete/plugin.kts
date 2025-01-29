import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.completion.CompletionService
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFile
import liveplugin.registerAction
import liveplugin.show
import java.util.*

class TabIndentOrCompleteAction: AnAction(), DumbAware {
    val actionManager = ActionManager.getInstance()

    fun performAction(event: AnActionEvent, name: String) {
        val dataContext = event.dataContext
        val inputEvent = event.inputEvent

        val action = actionManager.getAction(name)
        if (action != null) {
            val actionEvent = AnActionEvent.createEvent(action, dataContext, null, event.place, ActionUiKind.NONE, inputEvent)
            action.actionPerformed(actionEvent)
        }
    }

    fun indent(event: AnActionEvent) {
        performAction(event, "AutoIndentLines")
    }

    fun emacsIndent(event: AnActionEvent) {
        performAction(event, "EmacsStyleIndent")
    }

    fun complete(event: AnActionEvent) {
        performAction(event, "CodeCompletion")
    }

    fun gotoLineStart(event: AnActionEvent) {
        performAction(event, "EditorLineStart")
    }

    fun insertTab(event: AnActionEvent) {
        performAction(event, "EditorTab")
    }

    fun escape(event: AnActionEvent) {
        performAction(event, "EditorEscape")
    }

    private fun isPythonFile(file: VirtualFile?): Boolean {
        if (file == null) return false
        val fileType = FileTypeManager.getInstance().getFileTypeByFile(file)
        return fileType.name.equals("Python", ignoreCase = true)
    }

    fun isCurrentLineEmpty(editor: Editor): Boolean {
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
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val caret = editor!!.caretModel.primaryCaret
        val positionStart = caret.visualPosition
        val startColumn = positionStart.column

        if (caret.hasSelection()) {
            // - if there is an active selection, auto indent the region and
            // then discard the active selection
            val selectionModel = editor.selectionModel
            emacsIndent(event)
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    // Make sure removal runs on the UI thread
                    ApplicationManager.getApplication().invokeLater {
                        selectionModel.removeSelection()
                    }
                }
            }, 50)
        } else if (isCurrentLineEmpty(editor)) {
            // - if we're at the startColumn after the emacsIndent then I just assume it
            // is an empty line and insertTab
            val completionService = CompletionService.getCompletionService()
            val completionIndicator = completionService.currentCompletion as? CompletionProgressIndicator
            if (completionIndicator == null) {
                insertTab(event)
                val document = editor.document
                val virtualFile = FileDocumentManager.getInstance().getFile(document)
                if (!isPythonFile(virtualFile)) {
                    emacsIndent(event)
                }

                /*val afterIndentColumn = editor.caretModel.primaryCaret.visualPosition.column
                if (afterIndentColumn <= startColumn) {
                    editor.caretModel.moveToVisualPosition(positionStart)
                }*/
            }
        } else {
            // - if there is no active selection we'll try to indent and complete
            // - first gotoLineStart is like pressing Home, it will move caret to the
            // beginning of the current indentation, or the start of the line otherwise
            gotoLineStart(event)

            val afterLineStartColumn = editor.caretModel.primaryCaret.visualPosition.column

            // - if gotoLineStart moved the caret to the beginning of the line, use it
            // again to move it back to the indentation
            if (afterLineStartColumn == 0) {
                gotoLineStart(event)
            }
            // - emacsIndent should indent according to the current language, or if the
            // indentation is already correct or the language is not supported then it
            // does nothing
            val document = editor.document
            val virtualFile = FileDocumentManager.getInstance().getFile(document)
            if (!isPythonFile(virtualFile)) {
                emacsIndent(event)
            }

            val afterIndentColumn = editor.caretModel.primaryCaret.visualPosition.column
            // - if we're still at the same position after emacsIndent, then it did nothing
            // and we can try to complete
            if (afterIndentColumn == afterLineStartColumn && startColumn != 0) {
                // - to complete we move caret back where we started and then complete
                editor.caretModel.moveToVisualPosition(positionStart)
                complete(event)
            }
        }
    }
}

registerAction(id = "TabIndentOrComplete", keyStroke = "TAB", action = TabIndentOrCompleteAction())
if (!isIdeStartup) show("Enabled TabIndentOrComplete")
