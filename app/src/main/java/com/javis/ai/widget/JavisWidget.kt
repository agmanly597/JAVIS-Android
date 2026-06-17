package com.javis.ai.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.javis.ai.R
import com.javis.ai.services.JavisAssistantService

/**
 * 1x1 home screen widget — tap to activate JAVIS.
 * This is the overlay alternative for devices that don't support SYSTEM_ALERT_WINDOW.
 * Place on home screen → tap anywhere you see the J → JAVIS voice activates.
 */
class JavisWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val activateIntent = Intent(context, JavisAssistantService::class.java).apply {
                action = JavisAssistantService.ACTION_ACTIVATE
                putExtra("source", "widget")
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(context, widgetId, activateIntent, flags)
            } else {
                PendingIntent.getService(context, widgetId, activateIntent, flags)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_javis)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
