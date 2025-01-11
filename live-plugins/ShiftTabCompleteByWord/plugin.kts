import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import liveplugin.registerAction
import liveplugin.show

class ShiftTabCompleteByWord: AnAction(), DumbAware {
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

    fun completeByWord(event: AnActionEvent) {
        performAction(event, "InsertInlineCompletionWordAction")
    }

    fun suggestAiCompletion(event: AnActionEvent) {
        performAction(event, "CallInlineCompletionAction")
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            val caret = editor.caretModel!!.primaryCaret

            val positionStart = caret!!.visualPosition
            val startColumn = positionStart!!.column

            completeByWord(event)

            val positionAfterCompletionAction = caret!!.visualPosition
            val afterCompletionColumn = positionAfterCompletionAction!!.column

            if (afterCompletionColumn == startColumn) {
                suggestAiCompletion(event)
            }
        }
    }
}

registerAction(id = "ShiftTabCompleteByWord", keyStroke = "shift TAB", action = ShiftTabCompleteByWord())
if (!isIdeStartup) show("Enabled ShiftTabCompleteByWord")