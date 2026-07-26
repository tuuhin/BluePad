package com.sam.bluepad.presentation.feature_sketches.composables

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sam.bluepad.presentation.feature_sketches.events.CreateSketchState
import com.sam.bluepad.presentation.utils.transitions.SharedElementTransKeys
import com.sam.bluepad.presentation.utils.transitions.sharedBoundsWrapper
import com.sam.bluepad.presentation.utils.transitions.sharedTransitionSkipChildSize
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.add_content_screen_note_text
import com.sam.bluepad.resources.add_content_screen_title_text
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@Composable
fun CreateScreenContent(
    state: CreateSketchState,
    modifier: Modifier,
    sketchId: Uuid? = null,
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
        CreateSketchTextFields(
            state = state.contentTitleState,
            transitionKey = sketchId?.let { SharedElementTransKeys.sharedElementSketchTitle(it) },
            textStyle = MaterialTheme.typography.headlineMediumEmphasized,
            placeHolder = {
                Text(
                    text = stringResource(Res.string.add_content_screen_title_text),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            onKeyboardAction = { contentFocus.requestFocus() },
            focusRequester = titleFocus,
        )
        CreateSketchTextFields(
            state = state.contentTextState,
            transitionKey = sketchId?.let { SharedElementTransKeys.sharedElementSketchContent(it) },
            textStyle = MaterialTheme.typography.bodyMediumEmphasized,
            placeHolder = {
                Text(
                    text = stringResource(Res.string.add_content_screen_note_text),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            focusRequester = contentFocus,
            onKeyboardAction = { focusRequester.clearFocus() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}


@Composable
private fun CreateSketchTextFields(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Unit = {},
    onKeyboardAction: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    textStyle: TextStyle = MaterialTheme.typography.bodyMediumEmphasized,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    focusRequester: FocusRequester = FocusRequester(),
    transitionKey: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    BasicTextField(
        state = state,
        textStyle = textStyle.copy(color = textColor),
        keyboardOptions = keyboardOptions,
        onKeyboardAction = { onKeyboardAction() },
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorator = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = state.text.toString(),
                innerTextField = {
                    Box(
                        modifier = Modifier.then(
                            if (transitionKey == null) Modifier
                            else Modifier.sharedTransitionSkipChildSize()
                                .sharedBoundsWrapper(
                                    key = transitionKey,
                                    enter = scaleIn(
                                        transformOrigin = TransformOrigin(
                                            0f,
                                            .5f,
                                        ),
                                    ) + fadeIn(initialAlpha = .25f),
                                    exit = scaleOut(
                                        transformOrigin = TransformOrigin(
                                            0f,
                                            0f,
                                        ),
                                    ) + fadeOut(targetAlpha = .2f),
                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                    boundsTransform = { _, _ ->
                                        tween(durationMillis = 200, easing = EaseInOutBounce)
                                    },
                                    placeHolderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize,
                                ),
                        ),
                    ) {
                        innerTextField()
                    }
                },
                enabled = true,
                singleLine = false,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                placeholder = placeHolder,
                colors = TextFieldDefaults.noColor(),
                container = {},
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}

@Composable
private fun TextFieldDefaults.noColor(): TextFieldColors = colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    errorCursorColor = MaterialTheme.colorScheme.error,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
)
