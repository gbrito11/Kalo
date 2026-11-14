package pt.guilherme.fit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pt.guilherme.fit.fmt
import kotlin.math.roundToInt

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null || action != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    action?.invoke()
                }
            }
            content()
        }
    }
}

@Composable
fun CalorieRing(eaten: Double, target: Double, size: Dp = 132.dp) {
    val progress = if (target > 0) (eaten / target).toFloat() else 0f
    val track = MaterialTheme.colorScheme.surfaceVariant
    val over = progress > 1f
    val color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val stroke = 14.dp.toPx()
            drawArc(track, 135f, 270f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * progress.coerceIn(0f, 1f), false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val remaining = (target - eaten).roundToInt()
            Text(if (remaining >= 0) "$remaining" else "+${-remaining}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(if (remaining >= 0) "kcal restantes" else "kcal acima", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MacroBar(label: String, value: Double, target: Double, color: Color, unit: String = "g") {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text("${value.roundToInt()} / ${target.roundToInt()} $unit", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (target > 0) (value / target).toFloat().coerceIn(0f, 1f) else 0f },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
    }
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Campo numérico que aceita vírgula ou ponto. */
@Composable
fun NumberField(
    label: String,
    value: Double?,
    onValue: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    decimals: Int = 1,
) {
    var text by remember { mutableStateOf(value?.let { fmt(it, decimals) } ?: "") }
    // se o valor mudar por fora (ex.: botão "2 ovos"), atualiza o texto
    LaunchedEffect(value) {
        val parsed = text.replace(',', '.').toDoubleOrNull()
        if (value != null && (parsed == null || kotlin.math.abs(value - parsed) > 1e-6)) text = fmt(value, decimals)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { t ->
            val clean = t.filter { it.isDigit() || it == ',' || it == '.' }
            text = clean
            onValue(clean.replace(',', '.').toDoubleOrNull())
        },
        label = { Text(label) },
        suffix = if (suffix != null) { @Composable { Text(suffix) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MultiChips(options: List<T>, selected: Set<T>, label: (T) -> String, onChange: (Set<T>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(
                selected = o in selected,
                onClick = { onChange(if (o in selected) selected - o else selected + o) },
                label = { Text(label(o)) },
            )
        }
    }
}

/** Gráfico de linha simples (peso ao longo do tempo). */
@Composable
fun LineChart(values: List<Double>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, goal: Double? = null) {
    if (values.size < 2) {
        Text("Regista o peso mais vezes para veres a evolução.", style = MaterialTheme.typography.bodySmall)
        return
    }
    val goalColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val all = values + listOfNotNull(goal)
        val min = all.min() - 0.5
        val max = all.max() + 0.5
        fun y(v: Double) = (size.height * (1 - (v - min) / (max - min))).toFloat()
        val stepX = size.width / (values.size - 1)
        if (goal != null) {
            drawLine(goalColor.copy(alpha = 0.6f), Offset(0f, y(goal)), Offset(size.width, y(goal)), strokeWidth = 2.dp.toPx())
        }
        val path = Path()
        values.forEachIndexed { i, v ->
            val p = Offset(i * stepX, y(v))
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        values.forEachIndexed { i, v -> drawCircle(color, 3.5.dp.toPx(), Offset(i * stepX, y(v))) }
    }
}

@Composable
fun MacroLine(kcal: Double, p: Double, c: Double, f: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${kcal.roundToInt()} kcal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text("P ${p.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = FormaColors.protein)
        Text("H ${c.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = FormaColors.carbs)
        Text("G ${f.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = FormaColors.fat)
    }
}

@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))
