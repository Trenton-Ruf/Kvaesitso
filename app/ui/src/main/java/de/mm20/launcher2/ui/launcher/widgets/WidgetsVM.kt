package de.mm20.launcher2.ui.launcher.widgets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.mm20.launcher2.preferences.ui.UiSettings
import de.mm20.launcher2.widgets.RowWidget
import de.mm20.launcher2.widgets.Widget
import de.mm20.launcher2.widgets.WidgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class WidgetsVM(
    private val parentId: UUID?,
) : ViewModel(), KoinComponent {
    private val widgetRepository: WidgetRepository by inject()

    private val uiSettings: UiSettings by inject()

    val editButton = uiSettings.widgetEditButton
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val widgets = widgetRepository.get(parent = parentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun addWidget(widget: Widget, index: Int? = null) {
        viewModelScope.launch {
            val widgets = widgets.value.toMutableList()
            if (index == null) {
                widgets.add(widget)
            } else {
                widgets.add(index.coerceAtMost(widgets.size), widget)
            }
            widgetRepository.set(widgets, parentId)
        }
    }

    fun removeWidget(widget: Widget) {
        viewModelScope.launch {
            widgetRepository.delete(widget)
        }
    }

    fun updateWidget(widget: Widget) {
        viewModelScope.launch {
            widgetRepository.update(widget)
        }
    }

    fun moveUp(index: Int) {
        viewModelScope.launch {
            val widgets = widgets.value.toMutableList()
            val widget = widgets.removeAt(index)
            widgets.add(index - 1, widget)
            widgetRepository.set(widgets, parentId)
        }
    }

    fun moveDown(index: Int) {
        viewModelScope.launch {
            val widgets = widgets.value.toMutableList()
            val widget = widgets.removeAt(index)
            widgets.add(index + 1, widget)
            widgetRepository.set(widgets, parentId)
        }
    }

    fun combineIntoRow(originalWidget: Widget, newWidget: Widget) {
        if (originalWidget.isProtected || newWidget.isProtected) {
            return
        }
        viewModelScope.launch {
            if (originalWidget is RowWidget) {
                val children = widgetRepository.get(parent = originalWidget.id).first()
                widgetRepository.set(children + newWidget, originalWidget.id)
                return@launch
            }
            val currentWidgets = widgets.value.toMutableList()
            val index = currentWidgets.indexOfFirst { it.id == originalWidget.id }
            if (index == -1) return@launch

            val rowWidget = RowWidget(UUID.randomUUID())
            currentWidgets[index] = rowWidget
            widgetRepository.set(currentWidgets, parentId)
            widgetRepository.set(listOf(originalWidget, newWidget), rowWidget.id)
        }
    }

    fun removeFromRow(widget: Widget, rowId: UUID, targetParentId: UUID?) {
        viewModelScope.launch {
            widgetRepository.moveOutOfRow(widget, rowId, targetParentId)
        }
    }

    companion object : KoinComponent {
        fun Factory(parentId: String) = viewModelFactory {
            initializer {
                val id = try {
                    UUID.fromString(parentId)
                } catch (e: IllegalArgumentException) {
                    Log.e("WidgetsVM", "Invalid parentId: $parentId", e)
                    null
                }
                WidgetsVM(id)
            }
        }
    }
}
