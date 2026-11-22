package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.FocusNestApplication
import com.example.MainActivity
import com.example.R
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.SessionType
import com.example.domain.model.TimerMode
import com.example.service.FocusTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object WidgetUpdater {
  fun refreshAll(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val providers =
      listOf(
        TodayScreenTimeWidgetProvider::class.java,
        FocusTimerWidgetProvider::class.java,
        BlockingProfileWidgetProvider::class.java,
      )
    providers.forEach { cls ->
      val ids = manager.getAppWidgetIds(ComponentName(context, cls))
      if (ids.isNotEmpty()) {
        val intent =
          Intent(context, cls).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
          }
        context.sendBroadcast(intent)
      }
    }
  }
}

class TodayScreenTimeWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val app = context.applicationContext as? FocusNestApplication
        val today = FocusTimeUtils.todayKey()
        val usageMs =
          app?.container?.database?.appUsageDao()?.getUsageForDate(today)?.sumOf {
            it.foregroundTimeMs
          } ?: 0L
        val goalMin =
          app?.container?.database?.focusGoalDao()?.getGoal()?.dailyScreenTimeGoalMinutes ?: 180

        val openPending =
          PendingIntent.getActivity(
            context,
            201,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
          )
        appWidgetIds.forEach { id ->
          val views = RemoteViews(context.packageName, R.layout.widget_screen_time)
          views.setTextViewText(R.id.tv_screen_time, FocusTimeUtils.formatDurationMillis(usageMs))
          views.setTextViewText(
            R.id.tv_goal_progress,
            "Daily Goal: ${FocusTimeUtils.formatDurationMinutes(goalMin)}",
          )
          views.setOnClickPendingIntent(R.id.widget_root, openPending)
          appWidgetManager.updateAppWidget(id, views)
        }
      } finally {
        pending.finish()
      }
    }
  }
}

class FocusTimerWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val app = context.applicationContext as? FocusNestApplication
        val timer = app?.container?.preferences?.timerStateFlow?.first()
        val statusText =
          if (timer != null && timer.isRunning) {
            "${timer.phase.label} • ${timer.sessionType.label}"
          } else {
            "Ready • ${timer?.plannedMinutes ?: 25}m"
          }

        val quickFocusIntent =
          Intent(context, FocusTimerWidgetProvider::class.java).apply { action = ACTION_QUICK_FOCUS }
        val quickPending =
          PendingIntent.getBroadcast(
            context,
            202,
            quickFocusIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
          )
        val openPending =
          PendingIntent.getActivity(
            context,
            203,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
          )

        appWidgetIds.forEach { id ->
          val views = RemoteViews(context.packageName, R.layout.widget_focus_timer)
          views.setTextViewText(R.id.tv_focus_status, statusText)
          views.setTextViewText(
            R.id.btn_quick_focus,
            if (timer?.isRunning == true) "Open Timer" else "Start 25m",
          )
          views.setOnClickPendingIntent(
            R.id.btn_quick_focus,
            if (timer?.isRunning == true) openPending else quickPending,
          )
          views.setOnClickPendingIntent(R.id.widget_focus_root, openPending)
          appWidgetManager.updateAppWidget(id, views)
        }
      } finally {
        pending.finish()
      }
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (intent.action == ACTION_QUICK_FOCUS) {
      FocusTimerService.startTimer(
        context = context,
        mode = TimerMode.POMODORO,
        sessionType = SessionType.WORK,
        minutes = 25,
        note = "Started from Home Widget",
        profileId = null,
      )
    }
  }

  companion object {
    const val ACTION_QUICK_FOCUS = "com.example.focusnest.WIDGET_QUICK_FOCUS"
  }
}

class BlockingProfileWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val app = context.applicationContext as? FocusNestApplication
        val activeNames =
          app?.container?.database?.blockProfileDao()?.getAll()?.filter { it.isActive }?.map {
            it.name
          }.orEmpty()
        val analytics =
          app?.container?.database?.dailyAnalyticsDao()?.getByDate(FocusTimeUtils.todayKey())
        val openPending =
          PendingIntent.getActivity(
            context,
            204,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
          )

        appWidgetIds.forEach { id ->
          val views = RemoteViews(context.packageName, R.layout.widget_blocking_profile)
          views.setTextViewText(
            R.id.tv_profile_name,
            if (activeNames.isEmpty()) "Shield Standby" else activeNames.joinToString(", "),
          )
          views.setTextViewText(
            R.id.tv_blocked_attempts,
            "${analytics?.blockedAttemptsCount ?: 0} blocked attempts today",
          )
          views.setOnClickPendingIntent(R.id.widget_profile_root, openPending)
          appWidgetManager.updateAppWidget(id, views)
        }
      } finally {
        pending.finish()
      }
    }
  }
}
