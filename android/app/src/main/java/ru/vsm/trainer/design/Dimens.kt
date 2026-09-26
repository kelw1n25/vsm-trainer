package ru.vsm.trainer.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Отступы сайта для телефона: поле страницы 16, шаг блоков 24 (`.stack`), padding карточки 24. */
object Dimens {
    val gutter = 16.dp
    val stack = 24.dp
    val cardPadding = 24.dp
    val buttonHeight = 51.dp
    val buttonLargeHeight = 56.dp
    val inputHeight = 48.dp
}

/** Радиусы сайта: `--radius-lg` 20, `--radius` 16, кнопки и поля 12, картинки 14. */
object Shapes {
    val card = RoundedCornerShape(20.dp)
    val block = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(12.dp)
    val input = RoundedCornerShape(12.dp)
    val image = RoundedCornerShape(14.dp)
    val tab = RoundedCornerShape(10.dp)
    val tabs = RoundedCornerShape(14.dp)
    val pill = RoundedCornerShape(50)
    val storyBar = RoundedCornerShape(18.dp)
    val storyBox = RoundedCornerShape(24.dp)
    val storyChoice = RoundedCornerShape(18.dp)
    val storyEnd = RoundedCornerShape(28.dp)
    val nav = RoundedCornerShape(30.dp)
}
