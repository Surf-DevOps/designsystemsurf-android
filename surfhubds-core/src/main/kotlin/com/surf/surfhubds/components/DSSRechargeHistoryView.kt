package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/**
 * Port do `DSSRechargeHistoryView.swift` (iOS) — histórico de recargas agrupado por mês,
 * com seletor de mês (anterior/próximo) e lista de transações via RecyclerView.
 */
class DSSRechargeHistoryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    // MARK: - Modelos públicos

    data class Transacao(
        val tipo: String = "",
        val vlCredito: Double = 0.0,
        val dtExecucao: String = "",
    )

    data class MonthGroup(
        val mes: String,
        val transacoes: List<Transacao> = emptyList(),
    )

    /** Callback equivalente ao delegate `didChangeMonth`. */
    var onMonthChange: ((month: String) -> Unit)? = null

    // MARK: - Estado

    private var monthGroups: List<MonthGroup> = emptyList()
    private var currentIndex: Int = 0

    // MARK: - UI

    private val titleLabel = TextView(context).apply {
        text = "Histórico"
        textSize = 22f
        typeface = DSSFont.bold(context, 22f).typeface
    }

    private val monthHeaderView = MonthHeaderView(context)

    private val recyclerView = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context)
        isNestedScrollingEnabled = true
        overScrollMode = View.OVER_SCROLL_ALWAYS
    }

    private val emptyStateLabel = TextView(context).apply {
        text = "Sem recargas neste mês"
        textSize = 14f
        typeface = DSSFont.regular(context, 14f).typeface
        gravity = Gravity.CENTER
        visibility = View.GONE
    }

    private val adapter = HistoryAdapter()

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.TRANSPARENT)

        addView(
            titleLabel,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
        addView(
            monthHeaderView,
            LayoutParams(LayoutParams.MATCH_PARENT, 44f.dpToPx(context)).apply {
                topMargin = 16f.dpToPx(context)
            },
        )
        addView(
            emptyStateLabel,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 16f.dpToPx(context) + 24f.dpToPx(context)
            },
        )
        addView(
            recyclerView,
            LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = 16f.dpToPx(context)
            },
        )

        recyclerView.adapter = adapter

        monthHeaderView.onPrevious = { shiftMonth(1) }
        monthHeaderView.onNext = { shiftMonth(-1) }

        renderCurrentMonth()
        refresh()
        setupThemeObserver()
    }

    // MARK: - API pública

    fun configure(items: List<MonthGroup>) {
        monthGroups = items
        currentIndex = 0
        renderCurrentMonth()
    }

    // MARK: - Lógica de mês

    private fun shiftMonth(offset: Int) {
        val candidate = currentIndex + offset
        if (candidate < 0 || candidate >= monthGroups.size) return
        currentIndex = candidate
        renderCurrentMonth()
        onMonthChange?.invoke(monthGroups[currentIndex].mes)
    }

    private fun renderCurrentMonth() {
        if (monthGroups.isEmpty()) {
            monthHeaderView.setTitle("")
            monthHeaderView.setPreviousEnabled(false)
            monthHeaderView.setNextEnabled(false)
            rebuildList(emptyList())
            return
        }

        val group = monthGroups[currentIndex]
        monthHeaderView.setTitle(monthTitle(group))
        monthHeaderView.setPreviousEnabled(currentIndex < monthGroups.size - 1)
        monthHeaderView.setNextEnabled(currentIndex > 0)
        rebuildList(group.transacoes)
    }

    private fun rebuildList(items: List<Transacao>) {
        if (items.isEmpty()) {
            emptyStateLabel.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            adapter.submit(emptyList())
            return
        }
        emptyStateLabel.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        adapter.submit(items)
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val scheme = ThemeManager.colorScheme
        val isDark = scheme == ColorScheme.DARK || scheme == ColorScheme.BLACK
        // iOS: titleLabel.textColor = isDark ? .white : DSSColors.textPrimary
        titleLabel.setTextColor(if (isDark) Color.WHITE else DSSColors.textPrimary())
        // iOS: emptyStateLabel.textColor = isDark ? .lightGray : DSSColors.textSecondary
        emptyStateLabel.setTextColor(if (isDark) Color.LTGRAY else DSSColors.textSecondary())
        adapter.notifyDataSetChanged()
    }

    // MARK: - Adapter

    private inner class HistoryAdapter : RecyclerView.Adapter<RowViewHolder>() {

        private var items: List<Transacao> = emptyList()

        fun submit(newItems: List<Transacao>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
            val row = RechargeHistoryItemRowView(parent.context)
            row.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT,
            )
            return RowViewHolder(row)
        }

        override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
            val topMargin = if (position == 0) 0 else 12f.dpToPx(holder.itemView.context)
            (holder.itemView.layoutParams as? RecyclerView.LayoutParams)?.topMargin = topMargin
            holder.row.configure(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    private class RowViewHolder(val row: RechargeHistoryItemRowView) : RecyclerView.ViewHolder(row)

    // MARK: - Cabeçalho de mês

    private class MonthHeaderView(context: Context) : FrameLayout(context), ThemeAware {

        var onPrevious: (() -> Unit)? = null
        var onNext: (() -> Unit)? = null

        private val previousButton = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            isClickable = true
            isFocusable = true
        }
        private val nextButton = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            isClickable = true
            isFocusable = true
        }
        private val titleLabel = TextView(context).apply {
            textSize = 16f
            typeface = DSSFont.medium(context, 16f).typeface
            gravity = Gravity.CENTER
        }

        init {
            // O drawable de "voltar" é nomeado `ic_chevron_back` nas brands/app (não `ic_chevron_left`),
            // então sem o fallback o botão anterior ficava sem imagem (seta da esquerda some).
            previousButton.setImageDrawable(loadDrawable("ic_chevron_back", "ic_chevron_left"))
            nextButton.setImageDrawable(loadDrawable("ic_chevron_right"))

            addView(
                titleLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                },
            )
            addView(
                previousButton,
                LayoutParams(44f.dpToPx(context), 44f.dpToPx(context)).apply {
                    gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    marginStart = 16f.dpToPx(context)
                },
            )
            addView(
                nextButton,
                LayoutParams(44f.dpToPx(context), 44f.dpToPx(context)).apply {
                    gravity = Gravity.CENTER_VERTICAL or Gravity.END
                    marginEnd = 16f.dpToPx(context)
                },
            )

            previousButton.setOnClickListener { onPrevious?.invoke() }
            nextButton.setOnClickListener { onNext?.invoke() }

            refresh()
            setupThemeObserver()
        }

        fun setTitle(text: String) { titleLabel.text = text }

        // As setas só aparecem quando há navegação pertinente: no mês vigente (mais recente)
        // some a seta da direita (avançar); no mês mais antigo some a da esquerda (voltar).
        fun setPreviousEnabled(enabled: Boolean) {
            previousButton.isEnabled = enabled
            previousButton.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
        }

        fun setNextEnabled(enabled: Boolean) {
            nextButton.isEnabled = enabled
            nextButton.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
        }

        override fun applyTheme(theme: Theme) { refresh() }

        private fun refresh() {
            val scheme = ThemeManager.colorScheme
            val isDark = scheme == ColorScheme.DARK || scheme == ColorScheme.BLACK
            // iOS: primaryColor = isDark ? .white : DSSColors.textPrimary (título + tint dos chevrons)
            val primary = if (isDark) Color.WHITE else DSSColors.textPrimary()
            titleLabel.setTextColor(primary)
            previousButton.setColorFilter(primary, PorterDuff.Mode.SRC_IN)
            nextButton.setColorFilter(primary, PorterDuff.Mode.SRC_IN)
        }

        private fun loadDrawable(vararg names: String): android.graphics.drawable.Drawable? {
            for (name in names) {
                val resId = resources.getIdentifier(name, "drawable", context.packageName)
                if (resId != 0) return AppCompatResources.getDrawable(context, resId)
            }
            return null
        }
    }

    // MARK: - Célula

    private class RechargeHistoryItemRowView(context: Context) : FrameLayout(context), ThemeAware {

        private val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        private val iconContainer = FrameLayout(context)
        private val iconImageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(loadDrawable(context, "ic_arrow_up"))
        }
        private val titleLabel = TextView(context).apply {
            textSize = 15f
            typeface = DSSFont.medium(context, 15f).typeface
        }
        private val relativeTimeLabel = TextView(context).apply {
            textSize = 12f
            typeface = DSSFont.regular(context, 12f).typeface
            gravity = Gravity.END
        }
        private val descriptionLabel = TextView(context).apply {
            textSize = 13f
            typeface = DSSFont.regular(context, 13f).typeface
            setSingleLine(false)
        }
        private val validityLabel = TextView(context).apply {
            textSize = 13f
            typeface = DSSFont.regular(context, 13f).typeface
            setSingleLine(false)
        }

        init {
            val pad = 16f.dpToPx(context)

            iconContainer.addView(
                iconImageView,
                LayoutParams(16f.dpToPx(context), 16f.dpToPx(context)).apply {
                    gravity = Gravity.CENTER
                },
            )
            container.addView(
                iconContainer,
                LinearLayout.LayoutParams(40f.dpToPx(context), 40f.dpToPx(context)),
            )

            val topRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            topRow.addView(
                titleLabel,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
            topRow.addView(
                relativeTimeLabel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = 8f.dpToPx(context) },
            )

            val textStack = LinearLayout(context).apply { orientation = VERTICAL }
            textStack.addView(
                topRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            textStack.addView(
                descriptionLabel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 4f.dpToPx(context) },
            )
            textStack.addView(
                validityLabel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 4f.dpToPx(context) },
            )
            container.addView(
                textStack,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 12f.dpToPx(context)
                },
            )

            container.setPadding(pad, pad, pad, pad)
            addView(
                container,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )

            refresh()
            setupThemeObserver()
        }

        fun configure(item: Transacao) {
            titleLabel.text = RechargeHistoryItemPresenter.title(item)
            relativeTimeLabel.text = RechargeHistoryItemPresenter.relativeTime(item)
            descriptionLabel.text = RechargeHistoryItemPresenter.description(item)
            validityLabel.text = RechargeHistoryItemPresenter.validity(item)
        }

        override fun applyTheme(theme: Theme) { refresh() }

        private fun refresh() {
            val scheme = ThemeManager.colorScheme
            val isBlack = scheme == ColorScheme.BLACK
            val isDark = scheme == ColorScheme.DARK || isBlack

            // iOS: containerView.backgroundColor = .black ? .black : (isDark ? .secondarySystemBackground : DSSColors.surface)
            val containerBg = when {
                isBlack -> Color.BLACK
                isDark -> Color.rgb(28, 28, 30) // secondarySystemBackground (dark)
                else -> DSSColors.surface()
            }
            // iOS: borderColor = isDark ? systemGray4 : DSSColors.borderDefault
            val borderColor = if (isDark) {
                Color.rgb(58, 58, 60) // systemGray4 (dark)
            } else {
                DSSColors.borderDefault()
            }
            container.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = containerBg,
                cornerRadiusDp = 12f,
                strokeColor = borderColor,
                strokeWidthDp = 1f,
            )
            iconContainer.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = withAlpha(DSSColors.success(), 0.15f),
                cornerRadiusDp = 20f,
            )
            iconImageView.setColorFilter(DSSColors.success(), PorterDuff.Mode.SRC_IN)

            // iOS: primaryColor = isDark ? .white : DSSColors.textPrimary ; secondaryColor = isDark ? .lightGray : DSSColors.textSecondary
            val primary = if (isDark) Color.WHITE else DSSColors.textPrimary()
            val secondary = if (isDark) Color.LTGRAY else DSSColors.textSecondary()
            titleLabel.setTextColor(primary)
            descriptionLabel.setTextColor(primary)
            validityLabel.setTextColor(primary)
            relativeTimeLabel.setTextColor(secondary)
        }

        private fun withAlpha(color: Int, alpha: Float): Int {
            val a = (alpha * 255).toInt().coerceIn(0, 255)
            return (a shl 24) or (color and 0x00FFFFFF)
        }

        private fun loadDrawable(context: Context, name: String): android.graphics.drawable.Drawable? {
            val resId = resources.getIdentifier(name, "drawable", context.packageName)
            return if (resId != 0) AppCompatResources.getDrawable(context, resId) else null
        }
    }

    // MARK: - Apresentador (composição de texto)

    private object RechargeHistoryItemPresenter {

        fun title(item: Transacao): String = when (item.tipo.uppercase(Locale.getDefault())) {
            "RENOVACAO", "RENOVAÇÃO" -> "Renovação de programada"
            else -> "Recarga"
        }

        fun description(item: Transacao): String {
            val value = CurrencyFormatter.brl(item.vlCredito)
            return "Seu plano no valor de $value foi renovado!"
        }

        fun validity(item: Transacao): String {
            val executionDate = DateParser.parse(item.dtExecucao) ?: return ""
            val cal = Calendar.getInstance().apply {
                time = executionDate
                add(Calendar.DAY_OF_MONTH, 30)
            }
            return "plano válido até ${DateFormatters.shortBR.format(cal.time)}"
        }

        fun relativeTime(item: Transacao): String {
            val executionDate = DateParser.parse(item.dtExecucao) ?: return ""
            val nowMillis = Calendar.getInstance().timeInMillis
            val diffMillis = max(0L, nowMillis - executionDate.time)

            val days = diffMillis / (1000L * 60 * 60 * 24)
            // Acima de 30 dias: mostra em meses ("há 2M").
            if (days >= 30) return "há ${days / 30}M"
            // Entre 1 e 29 dias: mostra em dias ("há 3D").
            if (days >= 1) return "há ${days}D"

            // Menos de um dia: mostra as horas decorridas desde a recarga até agora ("há 5H").
            val hours = diffMillis / (1000L * 60 * 60)
            if (hours >= 1) return "há ${hours}H"

            // Menos de uma hora: mostra os minutos ("há 12min" / "agora").
            val minutes = diffMillis / (1000L * 60)
            return if (minutes >= 1) "há ${minutes}min" else "agora"
        }
    }

    // MARK: - Helpers

    private object DateParser {
        private val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
        )

        private val formatters: List<SimpleDateFormat> = formats.map { fmt ->
            SimpleDateFormat(fmt, Locale("en", "US")).apply {
                timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
                // iOS DateFormatter usa isLenient = true por padrão
                isLenient = true
            }
        }

        fun parse(string: String): Date? {
            if (string.isBlank()) return null
            for (formatter in formatters) {
                try {
                    return formatter.parse(string)
                } catch (_: Exception) {
                    // tenta o próximo formato
                }
            }
            return null
        }
    }

    private object CurrencyFormatter {
        private val formatter = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }

        fun brl(value: Double): String = try {
            formatter.format(value)
        } catch (_: Exception) {
            "R$ $value"
        }
    }

    private object DateFormatters {
        val shortBR: SimpleDateFormat =
            SimpleDateFormat("dd/MM/yy", Locale("pt", "BR"))
    }

    private fun monthTitle(group: MonthGroup): String {
        val mes = group.mes
        val monthName = if (mes.isEmpty()) {
            mes
        } else {
            mes.substring(0, 1).uppercase(Locale.getDefault()) + mes.substring(1)
        }
        val year = yearSuffix(group) ?: return monthName
        return "$monthName/$year"
    }

    private fun yearSuffix(group: MonthGroup): String? {
        for (transacao in group.transacoes) {
            val date = DateParser.parse(transacao.dtExecucao) ?: continue
            val cal = Calendar.getInstance().apply { time = date }
            val year = cal.get(Calendar.YEAR)
            return String.format(Locale.getDefault(), "%02d", year % 100)
        }
        return null
    }
}
