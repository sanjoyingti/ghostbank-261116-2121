package com.aibuilt.ghostbank.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aibuilt.ghostbank.model.StealthLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "ghostbank_settings")

/** Proxy exit as the user typed it. */
data class ProxyEntry(val raw: String) {
    val isSocks: Boolean get() = raw.startsWith("socks", ignoreCase = true)
    val hostPort: String get() = raw.substringAfter("://").trim()
    val scheme: String get() = if (isSocks) "socks5" else "http"
}

data class GhostSettings(
    val destinationAccount: String = "",
    val destinationIfsc: String = "",
    val stealthLevel: StealthLevel = StealthLevel.DEEP,
    val torMode: Boolean = false,
    val dohEnabled: Boolean = true,
    val killSwitch: Boolean = true,
    val proxyPool: List<ProxyEntry> = emptyList(),
    val diurnalWindow: Boolean = true,
    val windowStartHour: Int = 9,
    val windowEndHour: Int = 23,
    val maxRequestsPerMinute: Int = 60,
    val maxAccountsPerHour: Int = 40,
    val decoyBrowsing: Boolean = true,
    val ghostIdle: Boolean = true,
    val fragmentation: Boolean = true,
    val canaryEnabled: Boolean = true,
    val padding: Boolean = true,
    val geoMatch: Boolean = true,
    val exactOnePaisa: Boolean = true
) {
    /** Proxy-mandatory when the dial is Ghost. */
    val proxyRequired: Boolean
        get() = stealthLevel == StealthLevel.GHOST || (!torMode && proxyPool.isEmpty())

    val exitConfigured: Boolean
        get() = torMode || proxyPool.isNotEmpty()
}

class SettingsStore(private val context: Context) {

    private object Keys {
        val destinationAccount = stringPreferencesKey("destination_account")
        val destinationIfsc = stringPreferencesKey("destination_ifsc")
        val stealthLevel = stringPreferencesKey("stealth_level")
        val torMode = booleanPreferencesKey("tor_mode")
        val doh = booleanPreferencesKey("doh")
        val killSwitch = booleanPreferencesKey("kill_switch")
        val proxies = stringPreferencesKey("proxy_pool")
        val diurnal = booleanPreferencesKey("diurnal")
        val windowStart = intPreferencesKey("window_start")
        val windowEnd = intPreferencesKey("window_end")
        val maxRpm = intPreferencesKey("max_rpm")
        val maxAph = intPreferencesKey("max_aph")
        val decoy = booleanPreferencesKey("decoy")
        val idle = booleanPreferencesKey("idle")
        val fragmentation = booleanPreferencesKey("fragmentation")
        val canary = booleanPreferencesKey("canary")
        val padding = booleanPreferencesKey("padding")
        val geo = booleanPreferencesKey("geo")
        val exactPaisa = booleanPreferencesKey("exact_paisa")
    }

    val settings: Flow<GhostSettings> = context.settingsDataStore.data.map { p ->
        GhostSettings(
            destinationAccount = p[Keys.destinationAccount] ?: "",
            destinationIfsc = p[Keys.destinationIfsc] ?: "",
            stealthLevel = runCatching {
                StealthLevel.valueOf(p[Keys.stealthLevel] ?: StealthLevel.DEEP.name)
            }.getOrDefault(StealthLevel.DEEP),
            torMode = p[Keys.torMode] ?: false,
            dohEnabled = p[Keys.doh] ?: true,
            killSwitch = p[Keys.killSwitch] ?: true,
            proxyPool = (p[Keys.proxies] ?: "")
                .split("\n").map { it.trim() }.filter { it.isNotEmpty() }.map { ProxyEntry(it) },
            diurnalWindow = p[Keys.diurnal] ?: true,
            windowStartHour = p[Keys.windowStart] ?: 9,
            windowEndHour = p[Keys.windowEnd] ?: 23,
            maxRequestsPerMinute = p[Keys.maxRpm] ?: 60,
            maxAccountsPerHour = p[Keys.maxAph] ?: 40,
            decoyBrowsing = p[Keys.decoy] ?: true,
            ghostIdle = p[Keys.idle] ?: true,
            fragmentation = p[Keys.fragmentation] ?: true,
            canaryEnabled = p[Keys.canary] ?: true,
            padding = p[Keys.padding] ?: true,
            geoMatch = p[Keys.geo] ?: true,
            exactOnePaisa = p[Keys.exactPaisa] ?: true
        )
    }

    suspend fun snapshot(): GhostSettings = settings.first()

    suspend fun setDestination(account: String, ifsc: String) {
        context.settingsDataStore.edit {
            it[Keys.destinationAccount] = account
            it[Keys.destinationIfsc] = ifsc
        }
    }

    suspend fun setStealthLevel(level: StealthLevel) {
        context.settingsDataStore.edit { it[Keys.stealthLevel] = level.name }
    }

    suspend fun setTorMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.torMode] = enabled }
    }

    suspend fun setDoh(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.doh] = enabled }
    }

    suspend fun setKillSwitch(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.killSwitch] = enabled }
    }

    suspend fun setProxies(list: List<String>) {
        context.settingsDataStore.edit { it[Keys.proxies] = list.joinToString("\n") }
    }

    suspend fun setDiurnal(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.diurnal] = enabled }
    }

    suspend fun setWindow(startHour: Int, endHour: Int) {
        context.settingsDataStore.edit {
            it[Keys.windowStart] = startHour
            it[Keys.windowEnd] = endHour
        }
    }

    suspend fun setCaps(rpm: Int, aph: Int) {
        context.settingsDataStore.edit {
            it[Keys.maxRpm] = rpm.coerceIn(1, 300)
            it[Keys.maxAph] = aph.coerceIn(1, 500)
        }
    }

    suspend fun setDecoy(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.decoy] = enabled }
    }

    suspend fun setGhostIdle(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.idle] = enabled }
    }

    suspend fun setFragmentation(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.fragmentation] = enabled }
    }

    suspend fun setCanary(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.canary] = enabled }
    }

    suspend fun setPadding(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.padding] = enabled }
    }

    suspend fun setGeoMatch(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.geo] = enabled }
    }

    suspend fun setExactOnePaisa(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.exactPaisa] = enabled }
    }
}
