package com.majkeylab.seliacycles

import android.app.PendingIntent
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.LocaleList
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.LocaleManagerCompat
import androidx.core.net.toUri
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth

/** Native widgets never connect to, read, or write the external calendar provider. */
@SuppressLint("UseKtx") // Persistence failures must remain visible; KTX edit drops the commit result.
class CalendarWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateLock.withLock {
                    val prefs = preferences(context)
                    val ids = widgetIds(context)
                    when (intent.action) {
                        AppWidgetManager.ACTION_APPWIDGET_DELETED -> {
                            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                            check(prefs.edit().remove("profile_$id").remove("month_$id").remove("details_$id")
                                .remove("opacity_$id").remove("background_$id").remove("text_$id").remove("accent_$id").commit())
                        }
                        ACTION_MONTH -> {
                            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                            if (id !in ids) return@withLock
                            val offset = intent.getIntExtra("offset", 0)
                            if (offset !in -1..1) return@withLock
                            val month = if (offset == 0) YearMonth.now() else shownMonth(context, id).plusMonths(offset.toLong())
                            if (month !in YearMonth.from(DayLog.MIN_DATE)..YearMonth.from(DayLog.MAX_DATE)) return@withLock
                            check(prefs.edit().apply {
                                if (offset == 0) remove("month_$id") else putString("month_$id", month.toString())
                            }.commit())
                        }
                    }
                    updateWidgets(context, ids)
                }
            } catch (error: Exception) {
                Log.e("SeliaWidget", "Widget update failed", error)
            } finally { pending.finish() }
        }
    }

    companion object {
        private const val ACTION_MONTH = "com.majkeylab.seliacycles.WIDGET_MONTH"
        const val DAY_EXTRA = "widget_day"
        private val updateLock = Mutex()

        internal fun preferences(context: Context) = context.getSharedPreferences("calendar_widgets", Context.MODE_PRIVATE)
        private fun component(context: Context) = ComponentName(context, CalendarWidget::class.java)
        private fun widgetIds(context: Context) = AppWidgetManager.getInstance(context).getAppWidgetIds(component(context))

        internal fun ownsWidget(context: Context, id: Int): Boolean =
            AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider == component(context)

        internal fun shownMonth(context: Context, id: Int): YearMonth = runCatching {
            YearMonth.parse(preferences(context).getString("month_$id", null)).also { CalendarPaging.pageFor(it) }
        }.getOrDefault(YearMonth.now())

        internal fun localizedContext(context: Context): Context {
            val locales = LocaleManagerCompat.getApplicationLocales(context)
            return if (locales.isEmpty) context else context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
            })
        }

        internal fun appearance(context: Context, id: Int): WidgetAppearance = preferences(context).let {
            WidgetAppearance(
                opacityPercent = it.getInt("opacity_$id", 100).coerceIn(0, 100),
                backgroundRgb = it.getInt("background_$id", -1).takeIf { rgb -> rgb in 0..0xFFFFFF },
                textRgb = it.getInt("text_$id", -1).takeIf { rgb -> rgb in 0..0xFFFFFF },
                accentRgb = it.getInt("accent_$id", -1).takeIf { rgb -> rgb in 0..0xFFFFFF },
            )
        }

        internal suspend fun configure(context: Context, id: Int, profileId: String, details: Boolean,
            appearance: WidgetAppearance = WidgetAppearance()) = updateLock.withLock {
            require(ownsWidget(context, id))
            require(LocalProfiles(context).profiles().any { it.id == profileId })
            check(preferences(context).edit().putString("profile_$id", profileId).putBoolean("details_$id", details)
                .putInt("opacity_$id", appearance.opacityPercent).putInt("background_$id", appearance.backgroundRgb ?: -1)
                .putInt("text_$id", appearance.textRgb ?: -1).putInt("accent_$id", appearance.accentRgb ?: -1)
                .remove("month_$id").commit()) { "Could not save widget settings" }
            updateWidgets(context, intArrayOf(id))
        }

        suspend fun refresh(context: Context) = updateLock.withLock { updateWidgets(context, widgetIds(context)) }

        private fun updateWidgets(context: Context, ids: IntArray) {
            if (ids.isEmpty()) return
            val manager = AppWidgetManager.getInstance(context)
            val localized = localizedContext(context)
            ids.forEach { id ->
                val views = runCatching { loadViews(localized, id) }.getOrElse {
                    Log.e("SeliaWidget", "Could not load widget", it)
                    emptyViews(localized, id, R.string.widget_unavailable)
                }
                // Recheck after loading: never resurrect a widget removed by the launcher.
                if (ownsWidget(context, id)) manager.updateAppWidget(id, views)
            }
        }

        internal fun loadViews(context: Context, id: Int): RemoteViews {
            val prefs = preferences(context)
            val profileId = prefs.getString("profile_$id", null)
            val profile = LocalProfiles(context).profiles().firstOrNull { it.id == profileId }
                ?: return emptyViews(context, id, R.string.widget_choose_profile)
            // Missing/deleted profiles must never fall back to another person's records.
            if (!context.getDatabasePath(profileDatabaseName(profile.id)).exists()) {
                return emptyViews(context, id, R.string.widget_unavailable)
            }
            val content = CycleStore(context, profile.id).use { store ->
                CycleContent(store.load(), store.loadForecastSnapshots().associateBy(ForecastSnapshot::month))
            }
            return calendarWidgetViews(context, id, profile, content, shownMonth(context, id), prefs.getBoolean("details_$id", false), appearance(context, id))
        }

        internal fun configureIntent(context: Context, id: Int) = Intent(context, CalendarWidgetConfiguration::class.java)
            .setData("selia://widget/$id/configure".toUri())
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)

        internal fun configureClick(context: Context, id: Int): PendingIntent = PendingIntent.getActivity(
            context, 0, configureIntent(context, id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        internal fun openDay(context: Context, id: Int, profileId: String, day: LocalDate): PendingIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java)
                .setData("selia://widget/$id/$profileId/$day".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(ReminderWorker.PROFILE_ID_EXTRA, profileId).putExtra(DAY_EXTRA, day.toString()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        internal fun monthClick(context: Context, id: Int, offset: Int): PendingIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, CalendarWidget::class.java).setAction(ACTION_MONTH)
                .setData("selia://widget/$id/month/$offset".toUri())
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra("offset", offset),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun emptyViews(context: Context, id: Int, message: Int) = RemoteViews(context.packageName, R.layout.calendar_widget).apply {
            val colors = paletteColorScheme(AppPalette.OCEAN, CustomPalette(), false)
            setInt(R.id.widget_background, "setColorFilter", colors.surface.toArgb())
            setInt(R.id.widget_background, "setImageAlpha", 255)
            setTextColor(R.id.widget_brand, colors.onSurfaceVariant.toArgb())
            setTextColor(R.id.widget_message, colors.onSurface.toArgb())
            setTextColor(R.id.widget_settings, colors.primary.toArgb())
            removeAllViews(R.id.widget_weekdays)
            removeAllViews(R.id.widget_weeks)
            setTextViewText(R.id.widget_brand, context.getString(R.string.app_name))
            setTextViewText(R.id.widget_month, context.getString(R.string.nav_calendar))
            setContentDescription(R.id.widget_settings, context.getString(R.string.widget_configure))
            setViewVisibility(R.id.widget_message, View.VISIBLE)
            setTextViewText(R.id.widget_message, context.getString(message))
            setOnClickPendingIntent(R.id.widget_message, configureClick(context, id))
            setOnClickPendingIntent(R.id.widget_settings, configureClick(context, id))
            setViewVisibility(R.id.widget_navigation, View.GONE)
            setViewVisibility(R.id.widget_today, View.GONE)
        }
    }
}
