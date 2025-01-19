import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import liveplugin.registerAction
import liveplugin.show
import java.awt.datatransfer.StringSelection


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

    override fun actionPerformed(event: AnActionEvent) {
        performAction(event, "EditorUp")
        performAction(event, "EditorLineEnd")
        performAction(event, "EditorEnter")
        performAction(event, "\$Paste")
        performAction(event, "EmacsStyleIndent")

    }
}

// Register the YankIndent action and bind it to a specific shortcut
registerAction(id = "YankIndent", keyStroke = "ctrl Y", action = YankIndentAction())
if (!isIdeStartup) show("Enabled YankIndent")