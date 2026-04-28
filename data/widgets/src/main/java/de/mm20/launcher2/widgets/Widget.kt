package de.mm20.launcher2.widgets

import android.content.Context
import de.mm20.launcher2.database.entities.PartialWidgetEntity
import de.mm20.launcher2.database.entities.WidgetEntity
import de.mm20.launcher2.ktx.decodeFromStringOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

sealed class Widget {

    abstract val id: UUID
    open val isProtected: Boolean = true
    internal fun toDatabaseEntity(position: Int, parentId: UUID? = null): WidgetEntity {
        return toDatabaseEntity().let {
            WidgetEntity(
                id = it.id,
                type = it.type,
                config = it.config,
                position = position,
                parentId = parentId,
            )
        }
    }

    abstract fun getLabel(context: Context): String

    abstract fun toDatabaseEntity(): PartialWidgetEntity

    companion object {
        fun fromDatabaseEntity(entity: WidgetEntity): Widget? {
            return when (entity.type) {
                WeatherWidget.Type -> {
                    val config: WeatherWidgetConfig =
                        Json.decodeFromStringOrNull(entity.config?.takeIf { it.isNotBlank() })
                            ?: WeatherWidgetConfig()
                    WeatherWidget(entity.id, config)
                }
                MusicWidget.Type -> MusicWidget(
                    entity.id,
                    Json.decodeFromStringOrNull(entity.config?.takeIf { it.isNotBlank() })
                        ?: MusicWidgetConfig(),
                )
                CalendarWidget.Type -> {
                    val config: CalendarWidgetConfig =
                        Json.decodeFromStringOrNull(entity.config?.takeIf { it.isNotBlank() })
                            ?: CalendarWidgetConfig()
                    CalendarWidget(entity.id, config)
                }
                AppsWidget.Type -> {
                    val config: FavoritesWidgetConfig =
                        Json.decodeFromStringOrNull(entity.config?.takeIf { it.isNotBlank() })
                            ?: FavoritesWidgetConfig()
                    AppsWidget(entity.id, config)
                }
                AppWidget.Type -> {
                    val config: AppWidgetConfig =
                        Json.decodeFromStringOrNull(entity.config?.takeIf { it.isNotBlank() })
                            ?: return null
                    AppWidget(
                        entity.id,
                        config,
                    )
                }
                NotesWidget.Type -> {
                    val config: NotesWidgetConfig =
                        Json.decodeFromStringOrNull(entity.config?.takeIf { it.isNotBlank() })
                            ?: NotesWidgetConfig()
                    NotesWidget(entity.id, config)
                }
                RowWidget.Type -> {
                    val config: RowWidgetConfig =
                        Json.decodeFromStringOrNull(entity.config?.takeIf { it.isNotBlank() })
                            ?: RowWidgetConfig()
                    RowWidget(entity.id, config)
                }

                else -> null
            }
        }
    }
}

@Serializable
data class RowWidgetConfig(
    val height: Int = 120,
)

data class RowWidget(
    override val id: UUID,
    val config: RowWidgetConfig = RowWidgetConfig(),
) : Widget() {
    override val isProtected: Boolean = false
    override fun getLabel(context: Context): String {
        return context.getString(R.string.widget_name_row)
    }

    override fun toDatabaseEntity(): PartialWidgetEntity {
        return PartialWidgetEntity(
            id = id,
            type = Type,
            config = Json.encodeToString(config),
        )
    }

    companion object {
        const val Type = "row"
    }
}



enum class WidgetType(val value: String) {
    INTERNAL("internal"),
    THIRD_PARTY("3rdparty")
}