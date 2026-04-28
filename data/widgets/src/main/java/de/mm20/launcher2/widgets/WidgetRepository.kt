package de.mm20.launcher2.widgets

import androidx.room.withTransaction
import de.mm20.launcher2.backup.Backupable
import de.mm20.launcher2.crashreporter.CrashReporter
import de.mm20.launcher2.database.AppDatabase
import de.mm20.launcher2.database.entities.WidgetEntity
import de.mm20.launcher2.ktx.jsonObjectOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.util.UUID

interface WidgetRepository: Backupable {
    fun get(parent: UUID? = null, limit: Int = 100, offset: Int = 0): Flow<List<Widget>>
    suspend fun update(widget: Widget)
    suspend fun create(widget: Widget, position: Int, parentId: UUID? = null)
    suspend fun delete(widget: Widget)
    suspend fun set(widgets: List<Widget>, parentId: UUID? = null)

    suspend fun moveOutOfRow(widget: Widget, rowId: UUID, targetParentId: UUID?)

    fun exists(type: String): Flow<Boolean>
    fun count(type: String): Flow<Int>
}

internal class WidgetRepositoryImpl(
    private val database: AppDatabase,
) : WidgetRepository {

    override fun get(parent: UUID?, limit: Int, offset: Int): Flow<List<Widget>> {
        val dao = database.widgetDao()
        return if (parent == null) {
            dao.queryRoot(limit, offset)
        } else {
            dao.queryByParent(parent, limit, offset)
        }.map {
            it.mapNotNull { Widget.fromDatabaseEntity(it) }
        }
    }

    override suspend fun update(widget: Widget) = withContext(Dispatchers.IO) {
        val dao = database.widgetDao()
        dao.patch(widget.toDatabaseEntity())
    }

    override suspend fun create(widget: Widget, position: Int, parentId: UUID?) = withContext(Dispatchers.IO) {
        val dao = database.widgetDao()
        val entity = widget.toDatabaseEntity(position = position, parentId = parentId)
        dao.insert(entity)
    }

    override suspend fun delete(widget: Widget) = withContext(Dispatchers.IO) {
        val dao = database.widgetDao()
        database.withTransaction {
            if (widget is RowWidget) {
                dao.deleteByParent(widget.id)
            }
            dao.delete(widget.id)
        }
    }

    override suspend fun set(widgets: List<Widget>, parentId: UUID?) = withContext(Dispatchers.IO) {
        val dao = database.widgetDao()
        database.withTransaction {
            if (parentId == null) {
                dao.deleteRoot()
            } else {
                dao.deleteByParent(parentId)
            }
            dao.insert(widgets.mapIndexed { index, widget ->
                widget.toDatabaseEntity(position = index, parentId = parentId)
            })
        }
    }

    override suspend fun moveOutOfRow(widget: Widget, rowId: UUID, targetParentId: UUID?) = withContext(Dispatchers.IO) {
        val dao = database.widgetDao()
        database.withTransaction {
            // Find the row widget to get its position
            val rowEntity = dao.queryById(rowId) ?: return@withTransaction
            val rowPosition = rowEntity.position

            // Shift widgets in the target parent to make space
            if (targetParentId == null) {
                dao.shiftRoot(rowPosition, 1)
            } else {
                dao.shiftByParent(targetParentId, rowPosition, 1)
            }

            // Move the widget to the target parent at the row's position
            val entity = widget.toDatabaseEntity(position = rowPosition, parentId = targetParentId)
            dao.update(entity)

            // Check if the row is now empty or has only one widget
            val remainingCount = dao.countByParent(rowId)
            if (remainingCount <= 1) {
                if (remainingCount == 1) {
                    val lastWidgetEntity = dao.queryByParentSync(rowId).first()
                    val lastWidget = Widget.fromDatabaseEntity(lastWidgetEntity)
                    if (lastWidget != null) {
                        // Move last widget to position after the one we just moved
                        val lastWidgetTargetEntity = lastWidget.toDatabaseEntity(position = rowPosition + 1, parentId = targetParentId)
                        dao.update(lastWidgetTargetEntity)
                    }
                }
                dao.delete(rowId)
            }
        }
    }

    override fun exists(type: String): Flow<Boolean> {
        val dao = database.widgetDao()
        return dao.exists(type = type)
    }

    override fun count(type: String): Flow<Int> {
        val dao = database.widgetDao()
        return dao.count(type = type)

    }


    override suspend fun backup(toDir: File) = withContext(Dispatchers.IO) {
        val dao = database.backupDao()
        var page = 0
        do {
            val widgets = dao.exportWidgets(limit = 100, offset = page * 100)
            val jsonArray = JSONArray()
            for (widget in widgets) {
                jsonArray.put(
                    jsonObjectOf(
                        "config" to widget.config,
                        "position" to widget.position,
                        "type" to widget.type,
                        "id" to widget.id.toString(),
                        "parentId" to widget.parentId?.toString(),
                    )
                )
            }

            val file = File(toDir, "widgets2.${page.toString().padStart(4, '0')}")
            file.bufferedWriter().use {
                it.write(jsonArray.toString())
            }
            page++
        } while (widgets.size == 100)
    }

    override suspend fun restore(fromDir: File) = withContext(Dispatchers.IO) {
        val dao = database.backupDao()
        dao.wipeWidgets()

        val files =
            fromDir.listFiles { _, name -> name.startsWith("widgets2.") } ?: return@withContext

        for (file in files) {
            val widgets = mutableListOf<WidgetEntity>()
            try {
                val jsonArray = JSONArray(file.inputStream().reader().readText())

                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    val entity = WidgetEntity(
                        type = json.getString("type"),
                        position = json.getInt("position"),
                        config = json.optString("config"),
                        id = json.getString("id").let { UUID.fromString(it) },
                        parentId = json.optString("parentId").let { if (it.isEmpty()) null else UUID.fromString(it) }
                    )
                    widgets.add(entity)
                }

                dao.importWidgets(widgets)

            } catch (e: JSONException) {
                CrashReporter.logException(e)
            }
        }
    }
}