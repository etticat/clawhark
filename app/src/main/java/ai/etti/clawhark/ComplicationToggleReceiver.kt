package ai.etti.clawhark

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

class ComplicationToggleReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE = "ai.etti.clawhark.TOGGLE_RECORDING"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) return

        val prefs = context.getSharedPreferences(RecordingService.PREF_FILE, Context.MODE_PRIVATE)
        val shouldRecord = prefs.getBoolean(RecordingService.PREF_SHOULD_RECORD, false)

        if (shouldRecord) {
            prefs.edit().putBoolean(RecordingService.PREF_SHOULD_RECORD, false).apply()
            val stopIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            context.startService(stopIntent)
        } else {
            prefs.edit().putBoolean(RecordingService.PREF_SHOULD_RECORD, true).apply()
            context.startForegroundService(Intent(context, RecordingService::class.java))
        }

        ComplicationDataSourceUpdateRequester.create(
            context,
            ComponentName(context, RecordingComplicationService::class.java)
        ).requestUpdateAll()
    }
}
