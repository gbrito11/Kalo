package pt.guilherme.fit.domain

data class SimpleDate(val year: Int, val month: Int, val day: Int)

/** Datas guardadas como epochDay (dias desde 1970-01-01), sem dependências externas. */
object Dates {
    private val monthsShort = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
    val monthsLong = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
    )
    val weekdaysShort = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    val weekdaysLong = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")

    // Algoritmo de Howard Hinnant (days_from_civil / civil_from_days)
    fun toEpochDay(year: Int, month: Int, day: Int): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val m = month.toLong()
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    fun fromEpochDay(epochDay: Long): SimpleDate {
        val z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
        return SimpleDate((if (m <= 2) y + 1 else y).toInt(), m, d)
    }

    /** 1 = segunda ... 7 = domingo. 1970-01-01 foi uma quinta-feira. */
    fun weekday(epochDay: Long): Int = (DateMath.floorModCompat(epochDay + 3, 7) + 1).toInt()

    fun startOfWeek(epochDay: Long): Long = epochDay - (weekday(epochDay) - 1)

    fun daysInMonth(year: Int, month: Int): Int =
        (toEpochDay(if (month == 12) year + 1 else year, if (month == 12) 1 else month + 1, 1) - toEpochDay(year, month, 1)).toInt()

    fun short(epochDay: Long): String {
        val d = fromEpochDay(epochDay)
        return "${d.day} ${monthsShort[d.month - 1]}"
    }

    fun long(epochDay: Long): String {
        val d = fromEpochDay(epochDay)
        return "${weekdaysLong[weekday(epochDay) - 1]}, ${d.day} de ${monthsLong[d.month - 1].lowercase()}"
    }
}

internal object DateMath {
    fun floorModCompat(x: Long, y: Long): Long {
        val r = x % y
        return if (r < 0) r + y else r
    }
}
