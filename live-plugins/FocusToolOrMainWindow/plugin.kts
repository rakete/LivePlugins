import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindowManager
import liveplugin.registerAction
import liveplugin.show

class FocusToolOrMainWindowAction : AnAction(), DumbAware {
    val actionManager = ActionManager.getInstance()

    fun performAction(event: AnActionEvent, name: String) {
        val dataContext = event.dataContext
        val inputEvent = event.inputEvent

        val action = actionManager.getAction(name)
        if (action != null) {
            val actionEvent = AnActionEvent.createEvent(action, dataContext, null, event.place, ActionUiKind.NONE, inputEvent)
            action.actionPerformed(actionEvent)
        } else {
            show("Action not found: $name")
        }
    }

    fun focusTool(event: AnActionEvent) {
        performAction(event, "JumpToLastWindow")
    }

    fun focusEditor(event: AnActionEvent) {
        //performAction(event, "FocusEditor")
        val project = event.project
        if (project != null) {
            val fileEditorManager = FileEditorManager.getInstance(project)
            val selectedEditor = fileEditorManager.selectedTextEditor
            if (selectedEditor != null) {
                selectedEditor.contentComponent.requestFocus()
            }
        }
    }

    fun isEditorComponentInToolPanel(project: Project, component: EditorComponentImpl): Boolean {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindows = toolWindowManager.toolWindowIds

        for (toolWindowId in toolWindows) {
            val toolWindow = toolWindowManager.getToolWindow(toolWindowId)
            if (toolWindow != null && toolWindow.component.isAncestorOf(component)) {
                return true
            }
        }
        return false
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val focusManager = IdeFocusManager.getInstance(project)
            val focusedComponent = focusManager.focusOwner

            //show(focusedComponent)
            if (focusedComponent is EditorComponentImpl && !isEditorComponentInToolPanel(project, focusedComponent)) {
                focusTool(event)
            } else {
                focusEditor(event)
            }
        }
    }
}

registerAction(id = "FocusToolOrMainWindow", keyStroke = "F10", action = FocusToolOrMainWindowAction())
if (!isIdeStartup) show("Enabled FocusToolOrMainWindow")
