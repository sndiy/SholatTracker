package com.sholattracker.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Paint
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

        private val textIds = listOf(
            R.id.tvSholat1, R.id.tvSholat2, R.id.tvSholat3,
            R.id.tvSholat4, R.id.tvSholat5
        )
        private val shortNames = listOf("Sub", "Dzu", "Ash", "Mag", "Isy")

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val repo = SholatRepository(context)
            val record = repo.getRecord(repo.todayKey())
            val views = RemoteViews(context.packageName, R.layout.widget_sholat)

            val pi = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pi)
            views.setTextViewText(R.id.tvWidgetProgress, "${record.count}/5")

            SHOLAT_LIST.forEachIndexed { i, sholat ->
                val done = record.completed.contains(sholat.id)
                views.setTextViewText(textIds[i], shortNames[i])
                views.setTextColor(
                    textIds[i],
                    if (done) 0xFF3dba68.toInt() else 0xFF9aa5b4.toInt()
                )
                views.setInt(
                    textIds[i],
                    "setPaintFlags",
                    if (done) Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
                    else Paint.ANTI_ALIAS_FLAG
                )
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}