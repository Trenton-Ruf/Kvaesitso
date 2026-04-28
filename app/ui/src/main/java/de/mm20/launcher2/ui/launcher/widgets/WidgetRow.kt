package de.mm20.launcher2.ui.launcher.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.widgets.AppWidget
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((i, widget) in widgets.withIndex()) {
            key(widget.id) {
                val width = (widget as? AppWidget)?.config?.width?.takeIf { it > 0 }
                val isLast = i == widgets.lastIndex
                WidgetItem(
                    widget = widget,
                    editMode = editMode,
                    isInRow = true,
                    parentId = targetParentId,
                    modifier = (if (width != null && !isLast) Modifier.width(width.dp) else Modifier.weight(1f))
                        .fillMaxHeight(),
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
                    },
                    onAddToRow = { newWidget ->
                        viewModel.combineIntoRow(widget, newWidget)
                    }
                )
            }
        }
    }
}
