package nordligulv.com.googlefitwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.Log
import android.widget.RemoteViews
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.tasks.Tasks

class AppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAppWidget(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        getAlarmManager(context).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
                2 * DateUtils.MINUTE_IN_MILLIS, getAlarmIntent(context))
    }

    override fun onDisabled(context: Context) {
        getAlarmManager(context).cancel(getAlarmIntent(context))
    }

    private fun getAlarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getAlarmManager(context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    class AlarmReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val theWidget = ComponentName(context, AppWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(theWidget)
            updateAppWidget(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {

        const val TAG = "GoogleFitWidget"

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            Log.i(TAG, "updateAppWidget")

            val fitnessOptions = FitnessOptions.builder()
                    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                    .build()

            val gsa = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            val history = Fitness.getHistoryClient(context, gsa)

            Tasks.whenAllSuccess<DataSet>(
                    history.readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA),
                    history.readDailyTotal(DataType.TYPE_CALORIES_EXPENDED),
                    history.readDailyTotal(DataType.TYPE_DISTANCE_DELTA))

                    .addOnSuccessListener { dataSets ->
                        Log.d(TAG, "Data sets: $dataSets")

                        val views = RemoteViews(context.packageName, R.layout.app_widget)

                        views.setTextViewText(R.id.steps_text, getSteps(dataSets[0]))
                        views.setTextViewText(R.id.energy_text, getEnergy(dataSets[1]))
                        views.setTextViewText(R.id.distance_text, getDistance(dataSets[2]))

                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.fitness")
                        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, 0)
                        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                        appWidgetManager.updateAppWidget(appWidgetIds, views)
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "There was a problem getting the data", e) }
        }

        private fun getSteps(dataSet: DataSet): String {
            return if (dataSet.isEmpty)
                "0"
            else
                String.format("%,d", dataSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt())
        }

        private fun getEnergy(dataSet: DataSet): String {
            return if (dataSet.isEmpty)
                "0"
            else
                String.format("%,d", Math.round(dataSet.dataPoints[0].getValue(Field.FIELD_CALORIES).asFloat()))
        }

        private fun getDistance(dataSet: DataSet): String {
            return if (dataSet.isEmpty)
                "0"
            else
                String.format("%.2fkm", dataSet.dataPoints[0].getValue(Field.FIELD_DISTANCE).asFloat() / 1000)
        }
    }

}

