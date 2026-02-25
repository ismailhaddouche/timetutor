package com.haddouche.timetutor.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.haddouche.timetutor.util.NotificationHelper

class LessonReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val lessonTitle = inputData.getString("lessonTitle") ?: "Clase programada"
        val lessonTime = inputData.getString("lessonTime") ?: ""
        
        NotificationHelper.showNotification(
            applicationContext,
            "Recordatorio de clase",
            "Tu clase '$lessonTitle' comienza a las $lessonTime (en 10 min)."
        )

        return Result.success()
    }
}
