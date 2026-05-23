package com.sam.bluepad.presentation.navigation.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.sam.bluepad.presentation.navigation.utils.BottomSheetSceneStrategy.Companion.bottomSheet
import com.sam.bluepad.theme.Dimensions
import kotlinx.coroutines.launch

/** An [OverlayScene] that renders an [entry] within a [ModalBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
internal class BottomSheetScene<T : Any>(
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val modalBottomSheetProperties: ModalBottomSheetProperties,
    val isSkipPartiallyExpanded: Boolean = false,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {

        val scope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = isSkipPartiallyExpanded)

        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }
                    .invokeOnCompletion { onBack() }
            },
            sheetState = sheetState,
            properties = modalBottomSheetProperties,
        ) {
            Column(modifier = Modifier.padding(Dimensions.MODAL_BOTTOM_SHEET_CONTENT_PADDING)) {
                entry.Content()

            }
        }
    }
}

/**
 * A [SceneStrategy] that displays entries that have added [bottomSheet] to their [NavEntry.metadata]
 * within a [ModalBottomSheet] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        val bottomSheetProperties =
            lastEntry?.metadata?.get(BOTTOM_SHEET_PROPERTIES_KEY) as? ModalBottomSheetProperties
        val isSkipPartiallyExpanded = lastEntry?.metadata?.get(BOTTOM_SHEET_EXPANDED_KEY) as? Boolean ?: false

        return bottomSheetProperties?.let { properties ->
            @Suppress("UNCHECKED_CAST")
            BottomSheetScene(
                key = lastEntry.contentKey as T,
                previousEntries = entries.dropLast(1),
                overlaidEntries = entries.dropLast(1),
                entry = lastEntry,
                isSkipPartiallyExpanded = isSkipPartiallyExpanded,
                modalBottomSheetProperties = properties,
                onBack = onBack,
            )
        }
    }

    companion object {
        /**
         * Function to be called on the [NavEntry.metadata] to mark this entry as something that
         * should be displayed within a [ModalBottomSheet].
         *
         * @param properties properties that should be passed to the containing
         * [ModalBottomSheet].
         */
        @OptIn(ExperimentalMaterial3Api::class)
        fun bottomSheet(
            isSkipPartiallyExpanded: Boolean = false,
            properties: ModalBottomSheetProperties = ModalBottomSheetProperties()
        ): Map<String, Any> = mapOf(
            BOTTOM_SHEET_PROPERTIES_KEY to properties,
            BOTTOM_SHEET_EXPANDED_KEY to isSkipPartiallyExpanded,
        )

        internal const val BOTTOM_SHEET_PROPERTIES_KEY = "bottomsheet_key"
        internal const val BOTTOM_SHEET_EXPANDED_KEY = "bottomsheet_expanded"
    }
}
