package app.becoming.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import app.becoming.appContainer

/** Widget "+": cast today's vote. */
class VoteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.appContainer.repository.voteToday()
        BecomingWidget().updateAll(context)
    }
}

/** Widget "−": undo today's vote (past days are immutable). */
class UndoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.appContainer.repository.undoToday()
        BecomingWidget().updateAll(context)
    }
}
