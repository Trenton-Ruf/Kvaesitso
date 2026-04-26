package de.mm20.launcher2.ui.launcher.widgets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.widgets.AppWidget
import de.mm20.launcher2.widgets.AppsWidget
import de.mm20.launcher2.widgets.CalendarWidget
import de.mm20.launcher2.widgets.MusicWidget
import de.mm20.launcher2.widgets.NotesWidget
import de.mm20.launcher2.widgets.WeatherWidget
import java.util.UUID

@Composable
fun WidgetRow(
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    parentId: UUID,
    targetParentId: UUID?,
    height: Int,
) {
    val viewModel: WidgetsVM = viewModel(
        key = "widgets-row-$parentId",
        factory = WidgetsVM.Factory(parentId.toString()),
    )

    val widgets by viewModel.widgets.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
    ) {
        for ((i, widget) in widgets.withIndex()) {
            key(widget.id) {
                val width = when (widget) {
                    is AppWidget -> widget.config.width
                    is WeatherWidget -> widget.config.width
                    is MusicWidget -> widget.config.width
                    is CalendarWidget -> widget.config.width
                    is AppsWidget -> widget.config.width
                    is NotesWidget -> widget.config.width
                    else -> null
                }
                val isLast = i == widgets.lastIndex
                WidgetItem(
                    widget = widget,
                    editMode = editMode,
                    isInRow = true,
                    parentId = targetParentId,
                    modifier = (if (width != null && width > 0 && !isLast) Modifier.width(width.dp) else Modifier.weight(1f))
                        .fillMaxHeight()
                        .padding(start = if (i > 0) 8.dp else 0.dp),
                    onMoveLeft = {
                        if (i > 0) viewModel.moveUp(i)
                    },
                    onMoveRight = {
                        if (i < widgets.lastIndex) viewModel.moveDown(i)
                    },
                    onUngroup = {
                        viewModel.removeFromRow(widget, parentId, targetParentId)
                    },
                    onWidgetRemove = {
                        viewModel.removeWidget(widget)
                    },
                    onWidgetUpdate = {
                        viewModel.updateWidget(it)
                    }
                )
            }
        }
    }
}
