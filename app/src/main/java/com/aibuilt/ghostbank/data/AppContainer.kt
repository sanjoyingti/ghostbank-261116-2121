package com.aibuilt.ghostbank.data

import android.content.Context
import androidx.room.Room
import com.aibuilt.ghostbank.data.db.AccountDao
import com.aibuilt.ghostbank.data.db.GhostDatabase
import com.aibuilt.ghostbank.data.db.HarvestDao
import com.aibuilt.ghostbank.data.db.HostLogDao
import com.aibuilt.ghostbank.data.db.LedgerDao
import com.aibuilt.ghostbank.data.db.VirtualAccountDao
import com.aibuilt.ghostbank.data.net.GhostClient
import com.aibuilt.ghostbank.data.net.IfscResolver
import com.aibuilt.ghostbank.data.net.RedirectFollower
import com.aibuilt.ghostbank.data.settings.SettingsStore
import com.aibuilt.ghostbank.data.stealth.GhostProfileFactory
import com.aibuilt.ghostbank.engine.HarvestEngine
import com.aibuilt.ghostbank.engine.TraceAuditor

/**
 * Hand-rolled dependency holder. No DI framework, everything constructed once here.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    private val database: GhostDatabase = Room.databaseBuilder(
        appContext,
        GhostDatabase::class.java,
        DB_NAME
    ).fallbackToDestructiveMigration().build()

    val accountDao: AccountDao get() = database.accountDao()
    val harvestDao: HarvestDao get() = database.harvestDao()
    val ledgerDao: LedgerDao get() = database.ledgerDao()
    val hostLogDao: HostLogDao get() = database.hostLogDao()
    val virtualAccountDao: VirtualAccountDao get() = database.virtualAccountDao()

    val settingsStore = SettingsStore(appContext)

    val redirectFollower = RedirectFollower(maxHops = 5)

    val ghostClient = GhostClient(
        settings = settingsStore,
        redirectFollower = redirectFollower,
        hostLogDao = hostLogDao
    )

    val ifscResolver = IfscResolver(ghostClient)

    val profileFactory = GhostProfileFactory()

    val traceAuditor = TraceAuditor(ghostClient, redirectFollower)

    val harvestEngine = HarvestEngine(
        settings = settingsStore,
        ghostClient = ghostClient,
        ifscResolver = ifscResolver,
        profileFactory = profileFactory,
        harvestDao = harvestDao,
        ledgerDao = ledgerDao,
        virtualAccountDao = virtualAccountDao,
        accountDao = accountDao,
        hostLogDao = hostLogDao
    )

    companion object {
        const val DB_NAME = "ghostbank.db"
    }
}
