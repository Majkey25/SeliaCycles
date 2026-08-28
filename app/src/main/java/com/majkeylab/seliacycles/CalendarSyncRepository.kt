package com.majkeylab.seliacycles

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.DateTimeException
import java.time.LocalDate
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class PartnerCalendar(val ownerUid: String, val ownerName: String, val logs: List<DayLog>)

class CalendarSyncRepository(context: Context) {
    private val appContext = context.applicationContext

    val isConfigured: Boolean
        get() = FirebaseApp.getApps(appContext).isNotEmpty()

    suspend fun syncOwner(logs: List<DayLog>, ownerName: String) {
        val uid = currentUid()
        val database = firestore()
        val timestamp = Timestamp.now()
        val calendar = database.document("calendars/$uid")
        val existingCalendar = calendar.get().awaitResult()
        calendar.set(
            buildMap {
                put("ownerUid", uid)
                put("ownerName", ownerName.take(MAX_OWNER_NAME_LENGTH))
                put("updatedAt", timestamp)
                if (!existingCalendar.exists()) put("createdAt", timestamp)
            },
            SetOptions.merge(),
        ).awaitResult()

        val local = logs.filter(DayLog::bleeding).associateBy { it.day.toString() }
        val collection = database.collection("calendars/$uid/days")
        val remote = collection.get().awaitResult()
        check(remote.size() <= CycleBackup.MAX_LOGS) { "Remote calendar is too large" }
        val stale = remote.documents.filterNot { it.id in local }.map { it.reference }
        val writes = local.map { (day, log) ->
            collection.document(day) to (log.toPartnerPayload() + ("updatedAt" to timestamp))
        }
        commit(database, stale, writes)
    }

    suspend fun createInvitation(): String {
        val uid = currentUid()
        val token = PartnerInviteToken.generate()
        firestore().document("invitations/$token").set(mapOf(
            "ownerUid" to uid,
            "calendarId" to uid,
            "createdAt" to Timestamp.now(),
            "expiresAt" to Timestamp(Date(System.currentTimeMillis() + INVITE_LIFETIME_MILLIS)),
            "acceptedBy" to null,
        )).awaitResult()
        return token
    }

    suspend fun acceptInvitation(rawToken: String): String {
        val token = PartnerInviteToken.normalize(rawToken)
        val readerUid = currentUid()
        val database = firestore()
        val invitation = database.document("invitations/$token")
        return database.runTransaction { transaction ->
            val snapshot = transaction.get(invitation)
            val ownerUid = requireNotNull(snapshot.getString("ownerUid"))
            transaction.update(invitation, "acceptedBy", readerUid)
            transaction.set(database.document("memberships/${ownerUid}_$readerUid"), mapOf(
                "ownerUid" to ownerUid,
                "readerUid" to readerUid,
                "inviteToken" to token,
                "createdAt" to Timestamp.now(),
            ))
            ownerUid
        }.awaitResult()
    }

    suspend fun partnerCalendars(): List<PartnerCalendar> {
        val readerUid = currentUid()
        val memberships = firestore().collection("memberships")
            .whereEqualTo("readerUid", readerUid)
            .get()
            .awaitResult()
        check(memberships.size() <= MAX_PARTNERS) { "Too many partner calendars" }
        return memberships.documents.map { membership ->
            loadPartner(requireNotNull(membership.getString("ownerUid")))
        }
    }

    suspend fun readers(): List<String> {
        val ownerUid = currentUid()
        val memberships = firestore().collection("memberships")
            .whereEqualTo("ownerUid", ownerUid)
            .get()
            .awaitResult()
        check(memberships.size() <= MAX_PARTNERS) { "Too many readers" }
        return memberships.documents.map { requireNotNull(it.getString("readerUid")) }
    }

    suspend fun revoke(readerUid: String) {
        val ownerUid = currentUid()
        firestore().document("memberships/${ownerUid}_$readerUid").delete().awaitResult()
    }

    suspend fun deleteCloudCopy() {
        val ownerUid = currentUid()
        val database = firestore()
        val days = database.collection("calendars/$ownerUid/days").get().awaitResult()
        val memberships = database.collection("memberships").whereEqualTo("ownerUid", ownerUid).get().awaitResult()
        val invitations = database.collection("invitations").whereEqualTo("ownerUid", ownerUid).get().awaitResult()
        check(days.size() <= CycleBackup.MAX_LOGS && memberships.size() <= MAX_PARTNERS &&
            invitations.size() <= MAX_INVITATIONS
        ) { "Cloud calendar is too large to delete safely" }
        val references = buildList {
            days.documents.mapTo(this) { it.reference }
            memberships.documents.mapTo(this) { it.reference }
            invitations.documents.mapTo(this) { it.reference }
            add(database.document("calendars/$ownerUid"))
        }
        commit(database, references, emptyList())
    }

    private suspend fun loadPartner(ownerUid: String): PartnerCalendar {
        val database = firestore()
        val calendar = database.document("calendars/$ownerUid").get().awaitResult()
        val days = database.collection("calendars/$ownerUid/days").get().awaitResult()
        check(days.size() <= CycleBackup.MAX_LOGS) { "Partner calendar is too large" }
        val logs = days.documents.map { document ->
            try {
                val day = LocalDate.parse(requireNotNull(document.getString("day")))
                DayLog(
                    day = day,
                    bleeding = requireNotNull(document.getBoolean("bleeding")),
                    flow = Flow.valueOf(requireNotNull(document.getString("flow"))),
                )
            } catch (error: DateTimeException) {
                throw IllegalStateException("Invalid partner date", error)
            }
        }.sortedBy(DayLog::day)
        return PartnerCalendar(ownerUid, calendar.getString("ownerName").orEmpty().ifBlank { "Selia Cycles" }, logs)
    }

    private suspend fun commit(
        database: FirebaseFirestore,
        deletes: List<DocumentReference>,
        writes: List<Pair<DocumentReference, Map<String, Any>>>,
    ) {
        val operations = buildList<Pair<DocumentReference, Map<String, Any>?>> {
            deletes.forEach { add(it to null) }
            writes.forEach { add(it) }
        }
        operations.chunked(MAX_BATCH_OPERATIONS).forEach { chunk ->
            val batch = database.batch()
            chunk.forEach { (reference, value) ->
                if (value == null) batch.delete(reference) else batch.set(reference, value)
            }
            batch.commit().awaitResult()
        }
    }

    private fun currentUid(): String = requireNotNull(FirebaseAuth.getInstance().currentUser?.uid) { "Google sign-in required" }

    private fun firestore(): FirebaseFirestore {
        check(isConfigured) { "Google sync is not configured" }
        return FirebaseFirestore.getInstance()
    }

    companion object {
        private const val MAX_BATCH_OPERATIONS = 400
        private const val MAX_PARTNERS = 20
        private const val MAX_INVITATIONS = 100
        private const val MAX_OWNER_NAME_LENGTH = 80
        private const val INVITE_LIFETIME_MILLIS = 24 * 60 * 60 * 1_000L
    }
}

internal suspend fun <T> Task<T>.awaitResult(): T = suspendCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) continuation.resume(task.result)
        else continuation.resumeWithException(task.exception ?: IllegalStateException("Firebase operation failed"))
    }
}
