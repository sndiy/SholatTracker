package com.sholattracker.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sholattracker.app.R
import com.sholattracker.app.data.SHOLAT_LIST
import com.sholattracker.app.data.SholatRepository
import com.sholattracker.app.ui.MainActivity

class SholatWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val repo = SholatRepository(context)
            val record = repo.getRecord(repo.todayKey())
            val views = RemoteViews(context.packageName, R.layout.widget_sholat)

            // Tap buka app
            val pi = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pi)

            // Progress angka
            views.setTextViewText(R.id.tvWidgetProgress, "${record.count}/5")

            // Dots
            val dotIds = listOf(R.id.dot1, R.id.dot2, R.id.dot3, R.id.dot4, R.id.dot5)
            SHOLAT_LIST.forEachIndexed { i, sholat ->
                views.setImageViewResource(
                    dotIds[i],
                    if (record.completed.contains(sholat.id)) R.drawable.widget_dot_filled
                    else R.drawable.widget_dot_empty
                )
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
