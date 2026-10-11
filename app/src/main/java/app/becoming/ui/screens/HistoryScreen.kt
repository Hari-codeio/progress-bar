package app.becoming.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.becoming.R
import app.becoming.data.JourneyHistory
import app.becoming.data.JourneyStatus
import app.becoming.data.LevelRecordEntity
import app.becoming.domain.Levels
import app.becoming.domain.resolveIconKey
import app.becoming.ui.IdentityAvatar
import app.becoming.ui.formatDay
import app.becoming.ui.screen
import app.becoming.ui.theme.BecomingTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun HistoryScreen(journeys: List<JourneyHistory>, onBack: () -> Unit) {
    val c = BecomingTheme.colors
    val today = remember { LocalDate.now().toEpochDay() }
    Column(Modifier.screen(c.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), contentDescription = "Back", tint = c.text)
            }
            Text("Legacy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.text)
        }
        if (journeys.isEmpty()) {
            Text(
                "No journeys yet. Your first vote starts the record.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.muted,
                modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(journeys, key = { it.journey.id }) { history -> JourneyCard(history, today) }
        }
    }
}

@Composable
private fun JourneyCard(history: JourneyHistory, today: Long) {
    val c = BecomingTheme.colors
    val journey = history.journey
    val completed = journey.status == JourneyStatus.COMPLETED
    val total = history.voteDays.size
    val endDay = if (completed) journey.completedEpochDay ?: today else today
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IdentityAvatar(
                iconKey = resolveIconKey(journey.iconKey, journey.skill, journey.identityTitle),
                photoPath = journey.iconPath,
                size = 44.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(journey.identityTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.text)
                Text(
                    "${journey.skill} · started ${formatDay(journey.startEpochDay)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (completed) "Completed ${formatDay(endDay)} · $total votes" else "Active · $total votes",
            style = MaterialTheme.typography.labelLarge,
            color = if (completed) c.limeText else c.violetText,
        )
        Spacer(Modifier.height(16.dp))
        for (level in 1..Levels.MAX_LEVEL) {
            val record = history.records.firstOrNull { it.level == level }
            val isCurrent = !completed && record == null && level == journey.unlockedLevel
            LevelRow(level = level, record = record, isCurrent = isCurrent, totalVotes = total, isLast = level == Levels.MAX_LEVEL)
        }
        Spacer(Modifier.height(8.dp))
        Text("Last 12 weeks", style = MaterialTheme.typography.labelLarge, color = c.muted)
        Spacer(Modifier.height(8.dp))
        VoteHeatmap(voteDays = history.voteDays, startDay = journey.startEpochDay, endDay = endDay)
    }
}

/** One step of the level timeline: completed (record), current (in progress) or locked. */
@Composable
private fun LevelRow(level: Int, record: LevelRecordEntity?, isCurrent: Boolean, totalVotes: Int, isLast: Boolean) {
    val c = BecomingTheme.colors
    val done = record != null
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Column(
            Modifier
                .width(20.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val dot = Modifier
                .padding(top = 4.dp)
                .size(12.dp)
                .clip(CircleShape)
            Box(
                when {
                    done -> dot.background(c.lime)
                    isCurrent -> dot.border(2.dp, c.violet, CircleShape)
                    else -> dot.background(c.track)
                },
            )
            if (!isLast) {
                Box(
                    Modifier
                        .padding(vertical = 2.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(if (done) c.lime else c.track),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Level $level · ${Levels.name(level)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (done || isCurrent) c.text else c.muted,
                )
                if (!done && !isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Icon(painterResource(R.drawable.ic_lock), contentDescription = "Locked", tint = c.muted, modifier = Modifier.size(14.dp))
                }
            }
            val detail = when {
                record != null ->
                    "${record.title} · ${formatDay(record.startEpochDay)} – ${formatDay(record.endEpochDay)}\n" +
                        "${record.votes} votes · ${record.missedDays} missed"
                isCurrent && Levels.isComplete(totalVotes, level) -> "Complete · open Home to choose your next step"
                isCurrent -> "In progress · ${Levels.votesInLevel(totalVotes, level)} of ${Levels.levelSize(level)}"
                else -> "Locked"
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

/** 7 rows (Mon..Sun) x 12 week columns ending with the week of [endDay]. */
@Composable
private fun VoteHeatmap(voteDays: Set<Long>, startDay: Long, endDay: Long) {
    val c = BecomingTheme.colors
    val firstMonday = LocalDate.ofEpochDay(endDay)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .minusWeeks(11)
        .toEpochDay()
    val outside = c.track.copy(alpha = 0.35f)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (row in 0 until 7) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (col in 0 until 12) {
                    val day = firstMonday + col * 7 + row
                    val color = when {
                        day in voteDays -> c.heatVoted
                        day < startDay || day > endDay -> outside
                        else -> c.track
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendSwatch(c.heatVoted)
            Text(" Voted   ", style = MaterialTheme.typography.labelSmall, color = c.muted)
            LegendSwatch(c.track)
            Text(" Missed", style = MaterialTheme.typography.labelSmall, color = c.muted)
        }
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}
