package com.sam.bluepad.presentation.navigation.entries

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_create.AddOrUpdateSketchScreen
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_back
import org.jetbrains.compose.resources.painterResource

fun EntryProviderScope<NavKey>.createOrUpdateSketchesEntry(
	backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.AddOrUpdateRoute> {

	AddOrUpdateSketchScreen(
		navigation = {
			if (backStack.isNotEmpty()) {
				IconButton(onClick = { backStack.removeLastOrNull() }) {
					Icon(
						painter = painterResource(Res.drawable.ic_back),
						contentDescription = "Back Arrow"
					)
				}
			}
		},
	)
}