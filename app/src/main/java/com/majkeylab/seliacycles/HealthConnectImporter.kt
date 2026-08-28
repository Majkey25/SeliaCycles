package com.majkeylab.seliacycles

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

class HealthImportException(message: String) : Exception(message)

class HealthConnectImporter(private val context: Context) {
    val status: Int
        get() = HealthConnectClient.getSdkStatus(context)

    suspend fun importLogs(): List<DayLog> {
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            throw HealthImportException("Health Connect is unavailable")
        }
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.contains(HealthPermission.getReadPermission(MenstruationPeriodRecord::class))) {
            throw HealthImportException("Menstruation permission is missing")
        }
        val filter = TimeRangeFilter.between(
            DayLog.MIN_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(),
            Instant.now().plus(1, ChronoUnit.DAYS),
        )
        val days = mutableMapOf<LocalDate, Flow>()
        readAll(client, MenstruationPeriodRecord::class, filter).forEach { record ->
            val first = record.startTime.toLocalDate(record.startZoneOffset)
            val last = record.endTime.minusNanos(1).toLocalDate(record.endZoneOffset)
            val length = ChronoUnit.DAYS.between(first, last)
            if (length !in 0..31 || first !in DayLog.MIN_DATE..DayLog.MAX_DATE || last > DayLog.MAX_DATE) {
                throw HealthImportException("Invalid menstruation period")
            }
            (0..length).forEach { offset -> days.putIfAbsent(first.plusDays(offset), Flow.UNKNOWN) }
        }
        readAll(client, MenstruationFlowRecord::class, filter).forEach { record ->
            val day = record.time.toLocalDate(record.zoneOffset)
            if (day !in DayLog.MIN_DATE..DayLog.MAX_DATE) throw HealthImportException("Invalid flow date")
            days[day] = when (record.flow) {
                MenstruationFlowRecord.FLOW_LIGHT -> Flow.LIGHT
                MenstruationFlowRecord.FLOW_MEDIUM -> Flow.MEDIUM
                MenstruationFlowRecord.FLOW_HEAVY -> Flow.HEAVY
                else -> Flow.UNKNOWN
            }
        }
        if (days.size > CycleBackup.MAX_LOGS) throw HealthImportException("Too many menstruation records")
        return days.map { (day, flow) -> DayLog(day = day, bleeding = true, flow = flow) }
    }

    private suspend fun <T : Record> readAll(
        client: HealthConnectClient,
        type: KClass<T>,
        filter: TimeRangeFilter,
    ): List<T> {
        val records = mutableListOf<T>()
        val seenTokens = mutableSetOf<String>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = filter,
                    pageSize = PAGE_SIZE,
                    pageToken = pageToken,
                ),
            )
            records += response.records
            if (records.size > CycleBackup.MAX_LOGS) throw HealthImportException("Too many health records")
            pageToken = response.pageToken
            if (pageToken != null && !seenTokens.add(pageToken)) {
                throw HealthImportException("Health Connect repeated a page")
            }
        } while (pageToken != null)
        return records
    }

    private fun Instant.toLocalDate(offset: ZoneOffset?): LocalDate =
        if (offset == null) atZone(ZoneId.systemDefault()).toLocalDate() else atOffset(offset).toLocalDate()

    companion object {
        const val PAGE_SIZE = 1_000
        val permissions: Set<String> = setOf(
            HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
            HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
        )
    }
}
