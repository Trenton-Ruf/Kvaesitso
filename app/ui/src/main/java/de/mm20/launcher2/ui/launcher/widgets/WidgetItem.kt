package de.mm20.launcher2.ui.launcher.widgets

import android.appwidget.AppWidgetManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.LauncherCard
import de.mm20.launcher2.ui.component.dragndrop.DraggableItem
import de.mm20.launcher2.ui.component.dragndrop.LazyDragAndDropColumn
import de.mm20.launcher2.ui.component.dragndrop.rememberLazyDragAndDropListState
import de.mm20.launcher2.ui.launcher.sheets.ConfigureWidgetSheet
import de.mm20.launcher2.ui.launcher.sheets.WidgetPickerSheet
import de.mm20.launcher2.ui.launcher.widgets.calendar.CalendarWidget
import de.mm20.launcher2.ui.launcher.widgets.external.AppWidget
import de.mm20.launcher2.ui.launcher.widgets.favorites.AppsWidget
import de.mm20.launcher2.ui.launcher.widgets.music.MusicWidget
import de.mm20.launcher2.ui.launcher.widgets.notes.NotesWidget
import de.mm20.launcher2.ui.launcher.widgets.weather.WeatherWidget
import de.mm20.launcher2.ui.theme.transparency.transparency
import de.mm20.launcher2.widgets.AppWidget
import de.mm20.launcher2.widgets.CalendarWidget
import de.mm20.launcher2.widgets.AppsWidget
import de.mm20.launcher2.widgets.MusicWidget
import de.mm20.launcher2.widgets.NotesWidget
import de.mm20.launcher2.widgets.RowWidget
import de.mm20.launcher2.widgets.WeatherWidget
import de.mm20.launcher2.widgets.Widget
import java.util.UUID

@Composable
fun WidgetItem(
    widget: Widget,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    isInRow: Boolean = false,
    parentId: UUID? = null,
    onWidgetAdd: (widget: Widget, offset: Int) -> Unit = { _, _ -> },
    onWidgetUpdate: (widget: Widget) -> Unit = {},
    onWidgetRemove: () -> Unit = {},
    onAddToRow: (Widget) -> Unit = {},
    onMoveLeft: () -> Unit = {},
    onMoveRight: () -> Unit = {},
    onUngroup: () -> Unit = {},
    draggableState: DraggableState = rememberDraggableState {},
    onDragStopped: () -> Unit = {}
) {
    val context = LocalContext.current

    var configure by rememberSaveable { mutableStateOf(false) }
    var addNewWidgetToRow by rememberSaveable { mutableStateOf(false) }

    var isDragged by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(if (isDragged) 8.dp else 0.dp)

    val backgroundOpacity by animateFloatAsState(
        if (widget is AppWidget && !widget.config.background && !editMode) 0f else MaterialTheme.transparency.surface,
        label = "widgetCardBackgroundOpacity",
    )

    val configuredHeight = when (widget) {
        is AppWidget -> widget.config.height
        is WeatherWidget -> widget.config.height
        is MusicWidget -> widget.config.height
        is CalendarWidget -> widget.config.height
        is AppsWidget -> widget.config.height
        is NotesWidget -> widget.config.height
        is RowWidget -> widget.config.height
        else -> null
    }

    val configuredWidth = when (widget) {
        is AppWidget -> widget.config.width
        is WeatherWidget -> widget.config.width
        is MusicWidget -> widget.config.width
        is CalendarWidget -> widget.config.width
        is AppsWidget -> widget.config.width
        is NotesWidget -> widget.config.width
        else -> null
    }

    if (isInRow) {
        Column(
            modifier = modifier
        ) {
            AnimatedVisibility(editMode) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = widget.getLabel(LocalContext.current),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    IconButton(onClick = { onMoveLeft() }) {
                        Icon(
                            painterResource(R.drawable.chevron_backward_24px),
                            contentDescription = stringResource(R.string.menu_move_left)
                        )
                    }
                    IconButton(onClick = { onMoveRight() }) {
                        Icon(
                            painterResource(R.drawable.chevron_forward_24px),
                            contentDescription = stringResource(R.string.menu_move_right)
                        )
                    }
                    IconButton(onClick = { onUngroup() }) {
                        Icon(
                            painterResource(R.drawable.unarchive_24px),
                            contentDescription = stringResource(R.string.menu_ungroup)
                        )
                    }
                    IconButton(onClick = {
                        addNewWidgetToRow = true
                    }) {
                        Icon(
                            painterResource(R.drawable.splitscreen_right_20px),
                            contentDescription = stringResource(R.string.widget_action_add_to_row)
                        )
                    }
                    IconButton(onClick = { onWidgetRemove() }) {
                        Icon(
                            painterResource(R.drawable.delete_24px),
                            contentDescription = stringResource(R.string.widget_action_remove)
                        )
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                when (widget) {
                    is RowWidget -> {
                        WidgetRow(
                            editMode = editMode,
                            parentId = widget.id,
                            targetParentId = parentId,
                            height = widget.config.height,
                        )
                    }

                    is WeatherWidget -> {
                        WeatherWidget(widget, modifier = Modifier.fillMaxSize())
                    }

                    is MusicWidget -> {
                        MusicWidget(widget, modifier = Modifier.fillMaxSize())
                    }

                    is CalendarWidget -> {
                        CalendarWidget(widget, modifier = Modifier.fillMaxSize())
                    }

                    is AppsWidget -> {
                        AppsWidget(widget, modifier = Modifier.fillMaxSize())
                    }

                    is NotesWidget -> {
                        NotesWidget(
                            widget,
                            onWidgetAdd = onWidgetAdd,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is AppWidget -> {
                        AppWidget(
                            widget,
                            onWidgetUpdate = onWidgetUpdate,
                            onWidgetRemove = onWidgetRemove,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        return
    }

    LauncherCard(
        modifier = modifier
            .zIndex(if (isDragged) 1f else 0f)
            .then(if (configuredWidth != null && configuredWidth > 0 && !editMode) Modifier.width(configuredWidth.dp) else Modifier)
            .then(if (configuredHeight != null && !editMode) Modifier.height(configuredHeight.dp) else Modifier),
        elevation = elevation,
        backgroundOpacity = backgroundOpacity,
    ) {
        Column(modifier = if (editMode) Modifier.wrapContentHeight() else Modifier.fillMaxHeight()) {
            AnimatedVisibility(editMode) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(R.drawable.drag_indicator_24px),
                        contentDescription = null,
                        modifier = Modifier.draggable(
                            state = draggableState,
                            orientation = Orientation.Vertical,
                            startDragImmediately = true,
                            onDragStarted = {
                                isDragged = true
                            },
                            onDragStopped = {
                                isDragged = false
                                onDragStopped()
                            }
                        )
                    )
                    Text(
                        text = widget.getLabel(LocalContext.current),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    IconButton(onClick = {
                        addNewWidgetToRow = true
                    }) {
                        Icon(
                            painterResource(R.drawable.splitscreen_right_20px),
                            contentDescription = stringResource(R.string.widget_action_add_to_row)
                        )
                    }
                    IconButton(onClick = {
                        configure = true
                    }) {
                        Icon(
                            painterResource(R.drawable.tune_24px),
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                    IconButton(onClick = { onWidgetRemove() }) {
                        Icon(
                            painterResource(R.drawable.delete_24px),
                            contentDescription = stringResource(R.string.widget_action_remove)
                        )
                    }
                }
            }
            AnimatedVisibility(!editMode || widget is RowWidget, modifier = if (editMode) Modifier else Modifier.weight(1f)) {
                if (editMode && widget is RowWidget) {
                    val childViewModel: WidgetsVM = viewModel(
                        key = "widgets-row-edit-${widget.id}",
                        factory = WidgetsVM.Factory(widget.id.toString()),
                    )
                    val children by childViewModel.widgets.collectAsState()
                    val dragAndDropState = rememberLazyDragAndDropListState(
                        onItemMove = { from, to ->
                            if (from.index < to.index) {
                                childViewModel.moveDown(from.index)
                            } else {
                                childViewModel.moveUp(from.index)
                            }
                        }
                    )

                    LazyDragAndDropColumn(
                        state = dragAndDropState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 2000.dp) // Large enough to typically avoid internal scrolling
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        bidirectionalDrag = false
                    ) {
                        items(children, key = { it.id }) { child ->
                            DraggableItem(state = dragAndDropState, key = child.id) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.drag_indicator_24px),
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = child.getLabel(LocalContext.current),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = {
                                            childViewModel.removeFromRow(child, widget.id, parentId)
                                        }) {
                                            Icon(
                                                painterResource(R.drawable.unarchive_24px),
                                                contentDescription = stringResource(R.string.menu_ungroup)
                                            )
                                        }
                                        IconButton(onClick = {
                                            childViewModel.removeWidget(child)
                                        }) {
                                            Icon(
                                                painterResource(R.drawable.delete_24px),
                                                contentDescription = stringResource(R.string.widget_action_remove)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                } else {
                    when (widget) {
                        is RowWidget -> {
                            WidgetRow(
                                editMode = editMode,
                                parentId = widget.id,
                                targetParentId = parentId,
                                height = widget.config.height,
                            )
                        }

                        is WeatherWidget -> {
                            WeatherWidget(widget, modifier = Modifier.fillMaxSize())
                        }

                        is MusicWidget -> {
                            MusicWidget(widget, modifier = Modifier.fillMaxSize())
                        }

                        is CalendarWidget -> {
                            CalendarWidget(widget, modifier = Modifier.fillMaxSize())
                        }

                        is AppsWidget -> {
                            AppsWidget(widget, modifier = Modifier.fillMaxSize())
                        }

                        is NotesWidget -> {
                            NotesWidget(
                                widget,
                                onWidgetAdd = onWidgetAdd,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        is AppWidget -> {
                            AppWidget(
                                widget,
                                onWidgetUpdate = onWidgetUpdate,
                                onWidgetRemove = onWidgetRemove,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
    ConfigureWidgetSheet(
        expanded = configure,
        widget = widget,
        onWidgetUpdated = onWidgetUpdate,
        onDismiss = { configure = false },
    )
    WidgetPickerSheet(
        expanded = addNewWidgetToRow,
        onDismiss = { addNewWidgetToRow = false },
        onWidgetSelected = {
            onAddToRow(it)
            addNewWidgetToRow = false
        }
    )
}
