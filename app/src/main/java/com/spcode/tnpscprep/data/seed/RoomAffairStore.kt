package com.spcode.tnpscprep.data.seed

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.AppMetaEntity

class RoomAffairStore(private val db: AppDatabase) : AffairStore {
    private val dao = db.affairDao()

    override suspend fun storedVersion(): Int = dao.getMeta(KEY_VERSION)?.toIntOrNull() ?: 0

    /** One transaction: upsert, delete pack items the new pack dropped, record version + "data up to". User tables untouched. */
    override suspend fun replace(data: AffairData) = db.withTransaction {
        dao.upsertPack(data.items)
        (dao.packIds() - data.items.map { it.id }.toSet()).chunked(500).forEach { dao.deletePack(it) }
        dao.putMeta(AppMetaEntity(KEY_VERSION, data.version.toString()))
        dao.putMeta(AppMetaEntity(KEY_UP_TO, data.dataUpTo.toString()))
    }

    companion object {
        const val KEY_VERSION = "ca_version"
        const val KEY_UP_TO = "ca_data_up_to"
    }
}
