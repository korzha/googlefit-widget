package nordligulv.com.googlefitwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.Log
import android.widget.RemoteViews
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.HistoryClient
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.data.Value

class AppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        context.startService(Intent(context, WidgetUpdateService::class.java))
/*
        if (pendingIntent == null) {
            val intent = Intent(context, WidgetUpdateService::class.java)
            pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        getAlarmManager(context).setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
                15 * DateUtils.MINUTE_IN_MILLIS, pendingIntent)
*/

//        for (appWidgetId in appWidgetIds) {
//            updateAppWidget(context, appWidgetManager, appWidgetId)
//        }
    }

    override fun onEnabled(context: Context) {
        getAlarmManager(context).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
                15 * DateUtils.MINUTE_IN_MILLIS, getAlarmIntent(context))

//        context.startService(Intent(context, WidgetUpdateService::class.java))
/*
        val builder = JobInfo.Builder(UpdateJob.JOB_ID,
                ComponentName(context.applicationContext, UpdateJob::class.java))
        builder.setPeriodic(DateUtils.MINUTE_IN_MILLIS)

        if (getScheduler(context).schedule(builder.build()) <= 0) {
            Log.i(TAG, "!!!cant schedule")
        }
*/

/*
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.fitness")
        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, 0)
        val container = RemoteViews(context.packageName, R.layout.app_widget)
        container.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
*/
    }

/*
    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        if (intent?.action.equals(UpdateJob.JOB_TICK)) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, AppWidget::class.java))
            onUpdate(context, appWidgetManager, ids)
        }
    }
*/

    override fun onDisabled(context: Context) {
        getAlarmManager(context).cancel(getAlarmIntent(context))
//        context.stopService(Intent(context, WidgetUpdateService::class.java))
//        getScheduler(context).cancel(UpdateJob.JOB_ID)
    }

    private fun getAlarmIntent(context : Context) : PendingIntent {
        val intent = Intent(context, WidgetUpdateService::class.java)
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getAlarmManager(context: Context) : AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
/*
    private fun getScheduler(context: Context) : JobScheduler {
        return context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    }
*/

    class WidgetUpdateService : Service() {
        override fun onBind(intent: Intent): IBinder? {
            return null
        }

        override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
            updateAppWidget(this)
            return super.onStartCommand(intent, flags, startId)
        }
    }

/*
    class UpdateJob : JobService() {
        override fun onStopJob(p0: JobParameters?): Boolean {
            return false
        }

        override fun onStartJob(p0: JobParameters?): Boolean {
            Log.i(TAG, "Send job broadcast")
            sendBroadcast(Intent(JOB_TICK))
            return false
        }

        companion object {
            const val JOB_ID = 9
            const val JOB_TICK = "jobtickckckck"
        }
    }
*/

    companion object {

        const val TAG = "GoogleFitWidget"

        internal fun updateAppWidget(context: Context) {
            Log.i(TAG, "updateAppWidget")

            val fitnessOptions = FitnessOptions.builder()
                    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                    .build()

            val gsa = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

            val history = Fitness.getHistoryClient(context, gsa)

            update(context, history, R.id.steps_text, DataType.TYPE_STEP_COUNT_DELTA, Field.FIELD_STEPS, object : Formatter {
                override fun format(value: Value): String {
                    return String.format("%,d", value.asInt())
                }
            })
            update(context, history, R.id.energy_text, DataType.TYPE_CALORIES_EXPENDED, Field.FIELD_CALORIES, object : Formatter {
                override fun format(value: Value): String {
                    return String.format("%,d", Math.round(value.asFloat()))
                }
            })
            update(context, history, R.id.distance_text, DataType.TYPE_DISTANCE_DELTA, Field.FIELD_DISTANCE, object : Formatter {
                override fun format(value: Value): String {
                    return String.format("%.2fkm", value.asFloat() / 1000)
                }
            })
        }

        private fun update(context: Context,
                           history: HistoryClient, viewId: Int, dataType: DataType, field: Field, formatter: Formatter) {
            history
                    .readDailyTotal(dataType)
                    .addOnSuccessListener { dataSet ->
                        val total = (if (dataSet.isEmpty)
                            "0"
                        else
                            formatter.format(dataSet.dataPoints[0].getValue(field)))

                        Log.i(TAG, "Total: $total")
                        val views = RemoteViews(context.packageName, R.layout.app_widget)
                        views.setTextViewText(viewId, total)

                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.fitness")
                        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, 0)
                        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val theWidget = ComponentName(context, AppWidget::class.java)
                        appWidgetManager.updateAppWidget(theWidget, views)
//                        val appWidgetIds = appWidgetManager.getAppWidgetIds(theWidget)
//                        for (id in appWidgetIds) {
//                            appWidgetManager.updateAppWidget(id, views)
//                        }
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "There was a problem getting the count", e) }
        }
    }

    interface Formatter {
        fun format(value: Value): String
    }
}

