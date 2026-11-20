package com.example.mobilesbase.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mobilesbase.data.MobileDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

object Billing {

    const val REMIND_DAYS_BEFORE = 2

    private val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

    fun nextChargeDate(billingDay: Int, today: LocalDate = LocalDate.now()): LocalDate? {
        if (billingDay !in 1..31) return null
        var date = today.withDayOfMonth(minOf(billingDay, today.lengthOfMonth()))
        if (date.isBefore(today)) {
            val next = today.plusMonths(1)
            date = next.withDayOfMonth(minOf(billingDay, next.lengthOfMonth()))
        }
        return date
    }

    fun daysUntil(date: LocalDate?, today: LocalDate = LocalDate.now()): Long? =
        if (date == null) null else ChronoUnit.DAYS.between(today, date)

    fun format(date: LocalDate?): String = date?.format(formatter) ?: "—"
}

object Notify {

    private const val CHANNEL_ID = "billing_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания об оплате",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Проверка баланса и оплата за 2 дня до списания"
            manager.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, id: Int, title: String, text: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // разрешение на уведомления не выдано
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val cards = try {
            MobileDatabase.getDatabase(ctx).mobileDao().getAllCards().first()
        } catch (e: Exception) {
            return Result.retry()
        }

        val prefs = ctx.getSharedPreferences("billing_notify", Context.MODE_PRIVATE)
        val today = LocalDate.now()

        cards.forEach { card ->
            val date = Billing.nextChargeDate(card.billingDay, today) ?: return@forEach
            val left = Billing.daysUntil(date, today) ?: return@forEach
            if (left == Billing.REMIND_DAYS_BEFORE.toLong() || left == 0L) {
                val key = "notified_${card.phoneNumber}_$date"
                if (!prefs.getBoolean(key, false)) {
                    val title = if (left == 0L) {
                        "Сегодня списание: ${card.phoneNumber}"
                    } else {
                        "Через 2 дня списание: ${card.phoneNumber}"
                    }
                    val text = "Проверьте баланс и оплатите ${card.monthlyPayment} ₽ " +
                        "до ${Billing.format(date)}. Оператор ${card.operator}, " +
                        "SIM: ${card.simLocation}. Сейчас на счету ${card.balance} ₽."
                    Notify.show(ctx, card.id + 1000, title, text)
                    prefs.edit().putBoolean(key, true).apply()
                }
            }
        }
        return Result.success()
    }
}

object ReminderScheduler {

    private const val WORK_NAME = "billing_reminder_daily"

    fun schedule(context: Context) {
        Notify.ensureChannel(context)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun runNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().build())
    }
}
