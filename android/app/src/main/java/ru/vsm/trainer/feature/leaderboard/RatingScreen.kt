package ru.vsm.trainer.feature.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.data.remote.dto.LeaderboardPeriod
import ru.vsm.trainer.data.remote.dto.LeaderboardRow
import ru.vsm.trainer.data.remote.dto.LeaderboardScope
import ru.vsm.trainer.design.Palette
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.AnimatedNumber
import ru.vsm.trainer.design.components.InitialsAvatar
import ru.vsm.trainer.design.components.LevelProgress
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.PageTitle
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.UserAvatar
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.design.components.VsmTabs
import ru.vsm.trainer.design.components.riseIn

/** Рейтинг — `RatingPage`: «Ваше место», вкладки уровня и периода, строки с медалями за 1–3 места. */
@Composable
fun RatingScreen(model: RatingViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val scope by model.scope.collectAsStateWithLifecycle()
    val period by model.period.collectAsStateWithLifecycle()
    val level by model.level.collectAsStateWithLifecycle()
    val colors = Vsm.colors
    Page {
        item { PageTitle("Рейтинг") }
        val board = (state as? ScreenState.Content)?.value
        board?.me?.let { me ->
            item {
                VsmCard(background = Brush.linearGradient(listOf(colors.surface, colors.soft)), spacing = 16.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(88.dp, ring = 4.dp)
                        Column(Modifier.weight(1f)) {
                            Muted("Ваше место · ${board.title}")
                            Text(
                                buildAnnotatedString {
                                    append("${me.rank}")
                                    withStyle(SpanStyle(fontSize = 17.6.sp, color = colors.muted, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)) { append(" из ${board.participants}") }
                                },
                                style = VsmType.pageTitle.copy(fontSize = 41.6.sp, lineHeight = 46.sp), color = colors.brand,
                            )
                            Text(me.fullName, style = VsmType.body, color = colors.text)
                        }
                    }
                    Column {
                        Muted("Баллы за период")
                        AnimatedNumber(me.points, VsmType.kpiValue, colors.heading, suffix = " XP")
                    }
                    level?.let { LevelProgress(it) }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VsmTabs(LeaderboardScope.entries.map { it to it.title }, scope, { model.select(scope = it) })
                VsmTabs(LeaderboardPeriod.entries.map { it to it.title }, period, { model.select(period = it) })
            }
        }
        if (board == null) {
            item { ScreenContent(state, model::load) {} }
        } else {
            item {
                Text(
                    buildAnnotatedString {
                        append(board.title)
                        withStyle(SpanStyle(color = colors.muted)) { append(" · участников: ${board.participants}") }
                    },
                    style = VsmType.h2, color = colors.heading,
                )
            }
            itemsIndexed(board.rows, key = { _, row -> "${board.scope}-${board.period}-${row.employeeId}" }) { index, row -> RatingRow(row, index) }
        }
    }
}

@Composable
private fun RatingRow(row: LeaderboardRow, index: Int) {
    val colors = Vsm.colors
    val medal = when (row.rank) {
        1 -> Palette.rankTop1
        2 -> Palette.rankTop2
        3 -> Palette.rankTop3
        else -> null
    }
    Row(
        Modifier.fillMaxWidth().riseIn(index, stepMs = 55, durationMs = 400).clip(Shapes.image)
            .then(if (row.isMe) Modifier.background(colors.soft).border(1.5.dp, colors.brandBorder, Shapes.image) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(medal?.first ?: colors.tagBg), contentAlignment = Alignment.Center) {
            Text("${row.rank}", style = VsmType.bodyBold.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = medal?.second ?: colors.text)
        }
        if (row.isMe) UserAvatar(44.dp, ring = 0.dp) else InitialsAvatar(row.fullName, 44.dp)
        Column(Modifier.weight(1f)) {
            Text(row.fullName + if (row.isMe) " (вы)" else "", style = VsmType.bodyBold, color = colors.text)
            Text("${row.brigade} · ${row.depot} · ${row.levelTitle}", style = VsmType.small, color = colors.muted)
        }
        Text("${row.points} XP", style = VsmType.bodyBold.copy(fontSize = 17.6.sp), color = colors.heading)
    }
}
