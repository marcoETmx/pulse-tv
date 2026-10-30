package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Period
import java.util.Locale

/** Spanish, with numbers and money written as in Mexico ("1,234.50", "$139"). */
object SpanishStrings : PulseStrings {
    override val locale: Locale = Locale.forLanguageTag("es-MX")
    override val groupingSeparator = ','
    override val decimalSeparator = '.'
    override val compactUnits = listOf(1_000L to "K", 1_000_000L to "M")
    override val percentSuffix = "%"
    override val currencyAfterAmount = false
    override val monthDayPattern = "d MMM"
    override val monthDayYearPattern = "d MMM yyyy"
    override val weekdayMonthDayPattern = "EEE, d MMM"

    override fun period(period: Period): String = when (period) {
        Period.TODAY -> "Hoy"
        Period.LAST_7_DAYS -> "7 días"
        Period.LAST_15_DAYS -> "15 días"
        Period.LAST_30_DAYS -> "30 días"
        Period.THIS_YEAR -> "Este año"
    }

    override val connecting = "Conectando con DataFast…"
    override val loadFailedTitle = "No se pudo cargar DataFast"
    override val tryAgain = "Reintentar"
    override val noData = "No se pudieron cargar los datos"
    override fun updated(time: String) = "Actualizado $time"
    override val refreshing = "Actualizando…"
    override fun loadingPeriod(period: Period) = "Cargando ${inSentence(period)}…"
    override fun periodFailed(selected: Period, error: String, shown: Period, time: String) =
        "No se pudo cargar ${inSentence(selected)}: $error · mostrando ${inSentence(shown)}, de las $time"
    override fun refreshFailed(error: String, time: String) = "No se pudo actualizar: $error · mostrando datos de las $time"
    override fun problem(problem: AnalyticsProblem) = when (problem) {
        AnalyticsProblem.Unreachable -> "No se puede conectar con DataFast. Revisa la conexión a internet de la TV."
        AnalyticsProblem.TokenRejected -> "DataFast rechazó el token (401). Revisa DATAFAST_TOKEN en local.properties."
        AnalyticsProblem.MissingPermissions -> "El token de DataFast necesita los permisos analytics:read y websites:read (403)."
        is AnalyticsProblem.RateLimited -> "Se alcanzó el límite de DataFast (${problem.requestsPerMinute} solicitudes por minuto)."
        is AnalyticsProblem.ProviderError -> "Error de DataFast ${problem.status}" + problem.detail?.let { ": $it" }.orEmpty()
        AnalyticsProblem.UnexpectedResponse -> "DataFast envió una respuesta inesperada."
        AnalyticsProblem.Unknown -> "Algo salió mal al cargar el panel."
    }
    override val demoBadge = "DATOS DEMO"

    override val visitors = "Visitantes"
    override val sales = "Ventas"
    override val mixedCurrencies = "Varias monedas"
    override fun payments(count: Long, formattedCount: String) = "$formattedCount ${if (count == 1L) "pago" else "pagos"}"
    override val chartHint = "Presiona ▼ para explorar"
    override val chartHintExploring = "◀ ▶ para moverte en el tiempo"
    override val chartEmpty = "No hay visitas en este periodo"
    override fun visitorCount(count: Long, formattedCount: String) = "$formattedCount ${if (count == 1L) "visitante" else "visitantes"}"
    override fun salesAmount(money: String) = "$money en ventas"
    override fun weekOf(date: String) = "Semana del $date"

    override fun rankingTitle(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "Top productos"
        RankingOrder.SALES -> "Top por ventas"
    }
    override fun rankingAll(total: Int) = "$total ${if (total == 1) "producto" else "productos"}"
    override fun rankingRows(first: Int, last: Int, total: Int) = "$first–$last de $total"
    override val rankingScrollHint = "▲▼ para desplazarte"
    override val rankingBrowseHint = "▶ para recorrer"
    override val noWebsites = "Aún no hay sitios en esta cuenta de DataFast."

    override val menu = "Menú"
    override val autoRefresh = "Actualización automática"
    override fun refreshInterval(minutes: Int) = "Cada $minutes min"
    override val topProducts = "Top productos"
    override fun rankingOrder(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "Por visitantes"
        RankingOrder.SALES -> "Por ventas"
    }
    override val refreshNow = "Actualizar ahora"
    override val notLoadedYet = "Aún sin datos"
    override val statusRefreshing = "actualizando…"
    override val statusRetrying = "reintentando pronto"
    override fun statusNext(time: String) = "próxima $time"
    override val demoData = "Datos demo"
    override val languageSection = "Idioma"
    override val systemLanguage = "Idioma del sistema"
    override val languageName = "Español"

    /** A period's name inside a sentence: "Cargando 30 días…", "mostrando hoy". */
    private fun inSentence(period: Period) = period(period).lowercase(locale)
}
