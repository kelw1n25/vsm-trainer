package ru.vsm.trainer.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.design.Dimens

/**
 * Страница сайта (`.stack`): поле 16, блоки через 24, подвал в конце. Список ленивый —
 * длинные разделы (справочник, рейтинг) не рисуются целиком заранее.
 */
@Composable
fun Page(state: LazyListState = rememberLazyListState(), footer: Boolean = true, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(start = Dimens.gutter, end = Dimens.gutter, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.stack),
    ) {
        content()
        if (footer) item("footer") { SiteFooter() }
    }
}
