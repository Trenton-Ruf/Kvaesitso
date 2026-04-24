package de.mm20.launcher2.ui.launcher.widgets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

@Composable
fun WidgetRow(
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    parentId: UUID,
    targetParentId: UUID?,
) {
    val viewModel: WidgetsVM = viewModel(
        key = "widgets-row-$parentId",
        factory = WidgetsVM.Factory(parentId.toString()),
    )

    val widgets by viewModel.widgets.collectAsState()

    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        for ((i, widget) in widgets.withIndex()) {
            key(widget.id) {
                WidgetItem(
                    widget = widget,
                    editMode = editMode,
                    isInRow = true,
                    parentId = targetParentId,
                    modifier = Modifier
                        .weight(1f)
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
