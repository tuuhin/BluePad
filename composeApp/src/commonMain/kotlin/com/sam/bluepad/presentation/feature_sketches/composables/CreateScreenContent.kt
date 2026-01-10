package com.sam.bluepad.presentation.feature_sketches.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sam.bluepad.presentation.feature_sketches.events.CreateSketchState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.add_content_screen_note_text
import com.sam.bluepad.resources.add_content_screen_title_text
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateScreenContent(
	state: CreateSketchState,
	modifier: Modifier,
	contentPadding: PaddingValues = PaddingValues.Zero
) {
	val lifecycleOwner = LocalLifecycleOwner.current
	val focusRequester = LocalFocusManager.current

	val titleFocus = remember { FocusRequester() }
	val contentFocus = remember { FocusRequester() }


	LaunchedEffect(lifecycleOwner) {
		if (state.isNewContent) titleFocus.requestFocus()
	}

	Column(
		modifier = modifier.padding(contentPadding),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		TextField(
			state = state.contentTitleState,
			textStyle = MaterialTheme.typography.headlineSmallEmphasized,
			placeholder = {
				Text(
					text = stringResource(Res.string.add_content_screen_title_text),
					style = MaterialTheme.typography.headlineSmall,
					color = MaterialTheme.colorScheme.secondary
				)
			},
			colors = TextFieldDefaults.noColor(),
			keyboardOptions = KeyboardOptions(
				capitalization = KeyboardCapitalization.Words,
				imeAction = ImeAction.Next
			),
			onKeyboardAction = { contentFocus.requestFocus() },
			modifier = Modifier
				.fillMaxWidth()
				.focusRequester(titleFocus)
		)

		TextField(
			state = state.contentTextState,
			textStyle = MaterialTheme.typography.titleMedium,
			placeholder = {
				Text(
					text = stringResource(Res.string.add_content_screen_note_text),
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.secondary
				)
			},
			colors = TextFieldDefaults.noColor(),
			lineLimits = TextFieldLineLimits.MultiLine(),
			keyboardOptions = KeyboardOptions(
				capitalization = KeyboardCapitalization.Sentences,
				keyboardType = KeyboardType.Text,
				imeAction = ImeAction.Done
			),
			onKeyboardAction = { focusRequester.clearFocus() },
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f)
				.focusRequester(contentFocus)
		)
	}
}


@Composable
fun TextFieldDefaults.noColor(): TextFieldColors = colors(
	focusedContainerColor = Color.Transparent,
	unfocusedContainerColor = Color.Transparent,
	disabledContainerColor = Color.Transparent,
	focusedIndicatorColor = Color.Transparent,
	unfocusedIndicatorColor = Color.Transparent,
	cursorColor = MaterialTheme.colorScheme.secondary,
	errorCursorColor = MaterialTheme.colorScheme.error,
	focusedTextColor = MaterialTheme.colorScheme.onSurface
)