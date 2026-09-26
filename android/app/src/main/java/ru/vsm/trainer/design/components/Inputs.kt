package ru.vsm.trainer.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.design.Dimens
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType

/** Поле `input` сайта: 48, радиус 12, рамка 1.5 control-border, фокус — рамка brand и кольцо. */
@Composable
fun VsmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    password: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    tag: String? = null,
) {
    val colors = Vsm.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.let { Text(it, style = VsmType.tagStrong, color = colors.text) }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = VsmType.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.brand),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = source,
            modifier = Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier),
            decorationBox = { field ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(Dimens.inputHeight)
                        .then(if (focused) Modifier.border(3.dp, colors.brand.copy(alpha = 0.3f), Shapes.input) else Modifier)
                        .clip(Shapes.input)
                        .background(colors.inputBg)
                        .border(1.5.dp, if (focused) colors.brand else colors.controlBorder, Shapes.input)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) Text(placeholder, style = VsmType.body, color = colors.muted)
                    field()
                }
            },
        )
    }
}

/** Переключатель `.switch`: дорожка 46×26, бегунок 20, включён — brand; подпись справа. */
@Composable
fun VsmSwitch(checked: Boolean, onChange: (Boolean) -> Unit, title: String, hint: String, modifier: Modifier = Modifier) {
    val colors = Vsm.colors
    val track by animateColorAsState(if (checked) colors.brand else colors.trackStrong, label = "switch")
    val offset by animateDpAsState(if (checked) 20.dp else 0.dp, label = "thumb")
    Row(
        modifier.fillMaxWidth().clickable(role = Role.Switch) { onChange(!checked) }.semantics { selected = checked },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(46.dp).height(26.dp).clip(Shapes.pill).background(track)) {
            Box(
                Modifier.padding(3.dp).offset(x = offset).size(20.dp)
                    .shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White),
            )
        }
        Column {
            Text(title, style = VsmType.bodyBold, color = colors.text)
            Text("— $hint", style = VsmType.body, color = colors.muted)
        }
    }
}

/** Вкладки `.tabs`: белая плашка с рамкой и тенью, активная — фон brand. */
@Composable
fun <T> VsmTabs(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = Vsm.colors
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .vsmShadow(Shapes.tabs, 6.dp)
            .clip(Shapes.tabs)
            .background(colors.surface)
            .border(1.dp, colors.border, Shapes.tabs)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (key, title) ->
            val active = key == selected
            val background by animateColorAsState(if (active) colors.brand else Color.Transparent, label = "tab")
            Box(
                Modifier.height(40.dp).clip(Shapes.tab).background(background)
                    .semantics { this.selected = active }
                    .clickable(role = Role.Tab) { onSelect(key) }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(title, style = VsmType.bodyStrong, color = if (active) Color.White else colors.text)
            }
        }
    }
}
