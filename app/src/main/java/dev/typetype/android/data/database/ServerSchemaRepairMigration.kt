package dev.typetype.android.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object ServerSchemaRepairMigration : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val columns = db.query("PRAGMA table_info(`servers`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }

        createRepairedTable(db)
        copyServers(db, columns)
        db.execSQL("DROP TABLE `servers`")
        db.execSQL("ALTER TABLE `servers_repaired` RENAME TO `servers`")
    }

    private fun createRepairedTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `servers_repaired` (" +
                "`id` TEXT NOT NULL, `baseUrl` TEXT NOT NULL, `displayName` TEXT NOT NULL, " +
                "`addedAt` INTEGER NOT NULL, `tagline` TEXT, " +
                "`version` TEXT NOT NULL DEFAULT '', `revision` TEXT NOT NULL DEFAULT '', " +
                "`apiVersion` INTEGER NOT NULL DEFAULT 0, `logoUrl` TEXT, `bannerUrl` TEXT, " +
                "`supportedServicesCsv` TEXT NOT NULL DEFAULT '', `minAndroidClientVersion` TEXT, " +
                "`registrationAllowed` INTEGER NOT NULL DEFAULT 0, " +
                "`guestAllowed` INTEGER NOT NULL DEFAULT 0, " +
                "`localLoginEnabled` INTEGER NOT NULL DEFAULT 1, " +
                "`oidcEnabled` INTEGER NOT NULL DEFAULT 0, `oidcProviderName` TEXT, " +
                "`oidcAutoRedirect` INTEGER NOT NULL DEFAULT 0, " +
                "`youtubeRemoteLoginEnabled` INTEGER NOT NULL DEFAULT 0, " +
                "`youtubeRemoteLoginReady` INTEGER NOT NULL DEFAULT 0, " +
                "`youtubeRemoteLoginUnavailableReason` TEXT, PRIMARY KEY(`id`))",
        )
    }

    private fun copyServers(db: SupportSQLiteDatabase, columns: Set<String>) {
        val values = serverColumns.joinToString(", ") { column ->
            if (column.name in columns) "`${column.name}`" else column.fallback
        }
        db.execSQL(
            "INSERT INTO `servers_repaired` (${serverColumns.joinToString(", ") { "`${it.name}`" }}) " +
                "SELECT $values FROM `servers`",
        )
    }

    private data class ServerColumn(val name: String, val fallback: String)

    private val serverColumns = listOf(
        ServerColumn("id", "''"),
        ServerColumn("baseUrl", "''"),
        ServerColumn("displayName", "''"),
        ServerColumn("addedAt", "0"),
        ServerColumn("tagline", "NULL"),
        ServerColumn("version", "''"),
        ServerColumn("revision", "''"),
        ServerColumn("apiVersion", "0"),
        ServerColumn("logoUrl", "NULL"),
        ServerColumn("bannerUrl", "NULL"),
        ServerColumn("supportedServicesCsv", "''"),
        ServerColumn("minAndroidClientVersion", "NULL"),
        ServerColumn("registrationAllowed", "0"),
        ServerColumn("guestAllowed", "0"),
        ServerColumn("localLoginEnabled", "1"),
        ServerColumn("oidcEnabled", "0"),
        ServerColumn("oidcProviderName", "NULL"),
        ServerColumn("oidcAutoRedirect", "0"),
        ServerColumn("youtubeRemoteLoginEnabled", "0"),
        ServerColumn("youtubeRemoteLoginReady", "0"),
        ServerColumn("youtubeRemoteLoginUnavailableReason", "NULL"),
    )
}
