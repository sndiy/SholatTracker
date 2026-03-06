package com.sholattracker.app.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Call WidgetUpdateReceiver.send(context) dari mana saja
 * untuk refresh semua widget yang terpasang.
 */
class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, SholatWidget::class.java)
        )
        ids.forEach { id ->
            SholatWidget.updateWidget(context, manager, id)
        }
    }

    companion object {
        const val ACTION = "com.sholattracker.UPDATE_WIDGET"

        fun send(context: Context) {
            context.sendBroadcast(Intent(ACTION).setPackage(context.packageName))
        }
    }
}
