package com.majkeylab.seliacycles

import android.content.Context
import android.annotation.SuppressLint
import java.util.UUID

enum class UiMode { SIMPLE, STANDARD, DETAILED }

data class LocalProfile(val id: String, val name: String, val mode: UiMode = UiMode.STANDARD) {
    init {
        requireValidProfileId(id)
        require(name == name.trim() && name.length <= LocalProfiles.MAX_NAME_LENGTH)
        require(name.none(Char::isISOControl))
        require(id == LocalProfiles.DEFAULT_ID || name.isNotBlank())
    }
}

internal fun requireValidProfileId(id: String) {
    require(id == LocalProfiles.DEFAULT_ID ||
        (id.length == 36 && runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false))) {
        "Invalid local profile id"
    }
}

fun profileDatabaseName(id: String): String {
    requireValidProfileId(id)
    return if (id == LocalProfiles.DEFAULT_ID) "selia-cycles.db" else "selia-cycles-$id.db"
}

/** Local metadata only. Call writes on the IO dispatcher; each profile owns a separate database. */
@SuppressLint("UseKtx") // Check commit results; KTX edit discards the persistence result.
class LocalProfiles(context: Context) {
    private val context = context.applicationContext
    private val preferences = context.getSharedPreferences("local_profiles", Context.MODE_PRIVATE)

    fun profiles(): List<LocalProfile> = synchronized(lock) { readProfiles() }

    fun selected(): LocalProfile = synchronized(lock) {
        val profiles = readProfiles()
        profiles.firstOrNull { it.id == preferences.getString("selected", DEFAULT_ID) } ?: profiles.first()
    }

    fun create(name: String, mode: UiMode = UiMode.STANDARD): LocalProfile = synchronized(lock) {
        val profiles = readProfiles()
        require(profiles.size < MAX_PROFILES) { "Too many local profiles" }
        val profile = LocalProfile(UUID.randomUUID().toString(), name.trim(), mode)
        check(preferences.edit()
            .putStringSet("ids", profiles.drop(1).mapTo(mutableSetOf()) { it.id }.apply { add(profile.id) })
            .putString("name_${profile.id}", profile.name)
            .putString("mode_${profile.id}", profile.mode.name)
            .commit()) { "Could not save local profile" }
        profile
    }

    fun update(id: String, name: String, mode: UiMode): LocalProfile = synchronized(lock) {
        require(readProfiles().any { it.id == id }) { "Unknown local profile" }
        val profile = LocalProfile(id, name.trim(), mode)
        check(preferences.edit().putString("name_$id", profile.name).putString("mode_$id", mode.name).commit()) {
            "Could not save local profile"
        }
        profile
    }

    fun select(id: String) = synchronized(lock) {
        require(readProfiles().any { it.id == id }) { "Unknown local profile" }
        check(preferences.edit().putString("selected", id).commit()) { "Could not select local profile" }
    }

    /** Close the store and delete its database first; the original profile cannot be removed. */
    fun remove(id: String) = synchronized(lock) {
        require(id != DEFAULT_ID) { "Cannot remove the original profile" }
        val profiles = readProfiles()
        require(profiles.any { it.id == id }) { "Unknown local profile" }
        check(!context.getDatabasePath(profileDatabaseName(id)).exists()) { "Delete profile data before its metadata" }
        val editor = preferences.edit()
            .putStringSet("ids", profiles.filter { it.id != id && it.id != DEFAULT_ID }.mapTo(mutableSetOf()) { it.id })
            .remove("name_$id")
            .remove("mode_$id")
        if (preferences.getString("selected", DEFAULT_ID) == id) editor.putString("selected", DEFAULT_ID)
        check(editor.commit()) { "Could not remove local profile" }
    }

    private fun readProfiles(): List<LocalProfile> {
        val ids = preferences.getStringSet("ids", emptySet()).orEmpty()
        require(ids.size < MAX_PROFILES && DEFAULT_ID !in ids) { "Invalid local profile registry" }
        return (listOf(DEFAULT_ID) + ids.sorted()).map { id ->
            LocalProfile(
                id = id,
                name = preferences.getString("name_$id", "").orEmpty(),
                mode = UiMode.valueOf(preferences.getString("mode_$id", UiMode.STANDARD.name)!!),
            )
        }
    }

    companion object {
        const val DEFAULT_ID = "default"
        const val MAX_PROFILES = 12
        const val MAX_NAME_LENGTH = 40
        private val lock = Any()
    }
}
