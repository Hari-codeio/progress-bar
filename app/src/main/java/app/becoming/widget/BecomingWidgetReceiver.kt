package app.becoming.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import app.becoming.work.MidnightWorker

class BecomingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BecomingWidget()

    /** First widget placed: make sure the midnight refresh is scheduled. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MidnightWorker.schedule(context)
    }
}
