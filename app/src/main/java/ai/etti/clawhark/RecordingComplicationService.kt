package ai.etti.clawhark

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class RecordingComplicationService : SuspendingComplicationDataSourceService() {

    private fun shouldBeRecording(): Boolean {
        return getSharedPreferences(RecordingService.PREF_FILE, MODE_PRIVATE)
            .getBoolean(RecordingService.PREF_SHOULD_RECORD, false)
    }

    /**
     * Ensures the recording service is running if it should be.
     * Called on every complication request — this is the primary mechanism for
     * restarting recording after reboot/OOM kill. The system invokes this callback
     * to render the watch face, which happens reliably after boot (unlike
     * BOOT_COMPLETED which Wear OS may not deliver). Having an active complication
     * also grants the foreground service launch exemption on Android 12+.
     */
    private fun ensureServiceRunning() {
        if (!shouldBeRecording()) return
        try {
            startForegroundService(Intent(this, RecordingService::class.java))
        } catch (e: Exception) {
            AppLog.e("Complication", "Failed to restart recording service", e)
        }
    }

    private fun toggleIntent(): PendingIntent {
        val intent = Intent(this, ComplicationToggleReceiver::class.java).apply {
            action = ComplicationToggleReceiver.ACTION_TOGGLE
        }
        return PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return buildComplicationData(type, recording = true)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val recording = shouldBeRecording()
        ensureServiceRunning()
        return buildComplicationData(request.complicationType, recording)
    }

    private fun buildComplicationData(type: ComplicationType, recording: Boolean): ComplicationData? {
        val text = if (recording) "REC" else "OFF"
        val iconRes = if (recording) R.drawable.ic_rec_on else R.drawable.ic_rec_off
        val description = if (recording) "Recording" else "Stopped"
        val tap = toggleIntent()
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, iconRes)).build()
        val complicationText = PlainComplicationText.Builder(text).build()
        val descriptionText = PlainComplicationText.Builder(description).build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = complicationText,
                contentDescription = descriptionText
            )
                .setMonochromaticImage(icon)
                .setTapAction(tap)
                .build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = if (recording) 1f else 0f,
                min = 0f,
                max = 1f,
                contentDescription = descriptionText
            )
                .setText(complicationText)
                .setMonochromaticImage(icon)
                .setTapAction(tap)
                .build()

            else -> null
        }
    }
}
