package com.surf.surfhubds.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import java.util.concurrent.TimeUnit

/**
 * Port do `DSSPixPendentCardView` do iOS.
 *
 * Card de PIX pendente com duas variantes:
 *  - `collapsible = true`: header com chevron, ao tocar expande revelando o resumo
 *    da compra e botão de copiar código.
 *  - `collapsible = false`: card simples já totalmente expandido.
 *
 * Persiste estado em `SharedPreferences` (prefix `DSSPixPendentCard_*` ou
 * `DSSPixSimpleCard_*`) para reaparecer enquanto o timer não expirar.
 */
class DSSPixPendentCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    collapsible: Boolean = true,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Disparado quando o timer chega a zero. */
    var onTimeout: (() -> Unit)? = null

    /** Disparado ao tocar no card. */
    var onCardTapped: (() -> Unit)? = null

    private val collapsible: Boolean = collapsible
    private val prefsPrefix: String =
        if (collapsible) "DSSPixPendentCard" else "DSSPixSimpleCard"

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val iconImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private val titleLabel = TextView(context).apply {
        text = "Recarga com pagamento pendente"
        maxLines = 1
    }
    private val expandButton = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private val timeLabel = TextView(context).apply { text = "00:00:00" }
    private val subtitleLabel = TextView(context).apply {
        text = "clique aqui para finalizar sua recarga"
        textSize = 11f
        typeface = DSSFont.light(context, 11f).typeface
    }

    private val resumeCard = DSSResumeCard(context).apply {
        setCategoryLabels(number = "Número", offer = "Oferta", price = "Valor")
    }
    private val separator = View(context)
    private val copyButton = AppCompatButton(context).apply {
        text = "Copiar código"
        textSize = 16f
        typeface = DSSFont.medium(context, 16f).typeface
        isAllCaps = false
    }

    /** Pix icon — opcionalmente injetado pela app (ImageLoader nativo). */
    var pixIcon: Drawable? = null
        set(value) { field = value; iconImageView.setImageDrawable(value) }

    var cornerRadiusDp: Float = 12f
        set(value) { field = value; refresh() }

    private var msisdn: String? = null
    private var offer: String? = null
    private var priceInCents: Int? = null
    private var pixCode: String? = null
    private var customTitle: String? = null

    // Overrides de estilo (via configureStyle) — preservados em trocas de tema.
    @ColorInt
    private var titleColorOverride: Int? = null
    @ColorInt
    private var buttonColorOverride: Int? = null

    private var targetTimestamp: Long? = null
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateTimer()
            if (targetTimestamp != null) handler.postDelayed(this, 1_000L)
        }
    }

    private var isExpanded: Boolean = false

    private val prefs: SharedPreferences by lazy {
        context.applicationContext.getSharedPreferences(prefsPrefix, Context.MODE_PRIVATE)
    }

    init {
        visibility = View.GONE
        setupView()
        refresh()
        setupThemeObserver()
        loadPersistedData()
    }

    private fun setupView() {
        addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
        val pad = if (collapsible) 20f.dpToPx(context) else 16f.dpToPx(context)
        container.setPadding(pad, pad, pad, pad)

        // Header row
        val iconSize = if (collapsible) 30f.dpToPx(context) else 24f.dpToPx(context)
        headerRow.addView(
            iconImageView,
            LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = (if (collapsible) 12f else 8f).dpToPx(context)
            },
        )

        titleLabel.textSize = if (collapsible) 13f else 14f
        titleLabel.typeface = DSSFont.medium(
            context,
            if (collapsible) 13f else 14f,
        ).typeface
        headerRow.addView(
            titleLabel,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        if (collapsible) {
            expandButton.setImageDrawable(loadChevronDown())
            headerRow.addView(
                expandButton,
                LinearLayout.LayoutParams(24f.dpToPx(context), 24f.dpToPx(context)).apply {
                    marginStart = 12f.dpToPx(context)
                },
            )
        }

        container.addView(
            headerRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Time label
        timeLabel.textSize = if (collapsible) 16f else 14f
        timeLabel.typeface = if (collapsible) {
            DSSFont.bold(context, 16f).typeface
        } else {
            DSSFont.medium(context, 14f).typeface
        }
        container.addView(
            timeLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = if (collapsible) 16f.dpToPx(context) else 8f.dpToPx(context)
            },
        )

        if (collapsible) {
            container.addView(
                subtitleLabel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 8f.dpToPx(context) },
            )
        }

        if (!collapsible) resumeCard.borderWidthDp = 0f
        container.addView(
            resumeCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = if (collapsible) pad else 0 },
        )

        if (collapsible) {
            container.addView(
                separator,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f.dpToPx(context),
                ).apply { topMargin = pad },
            )
        }

        container.addView(
            copyButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50f.dpToPx(context),
            ).apply { topMargin = if (collapsible) pad else 20f.dpToPx(context) },
        )

        // Gestures
        container.setOnClickListener {
            if (collapsible) toggleExpanded()
            onCardTapped?.invoke()
        }
        expandButton.setOnClickListener { toggleExpanded() }
        copyButton.setOnClickListener { onCopyTapped() }

        if (collapsible) updateExpandedState(animate = false)

        if (pixIcon == null) iconImageView.setImageDrawable(PaymentMethodImages.pixIcon(context))
    }

    /**
     * Configura o card com os dados do PIX pendente.
     * Define o tempo em segundos que será convertido para formato HH:MM:SS.
     *
     * @param showTimer espelha o parâmetro do iOS (sem efeito visual: o timer
     *   é sempre exibido em ambas as plataformas).
     */
    fun configure(
        title: String? = null,
        msisdn: String,
        offer: String,
        priceInCents: Int,
        durationInSeconds: Int,
        pixCode: String? = null,
        showTimer: Boolean = false,
    ) {
        if (durationInSeconds <= 0) {
            visibility = View.GONE
            clearPersistedData()
            return
        }
        this.customTitle = title
        title?.let { titleLabel.text = it }
        this.msisdn = msisdn
        this.offer = offer
        this.priceInCents = priceInCents
        this.pixCode = pixCode

        resumeCard.configure(title = title, number = msisdn, offer = offer, priceInCents = priceInCents)
        targetTimestamp = System.currentTimeMillis() +
            TimeUnit.SECONDS.toMillis(durationInSeconds.toLong())

        visibility = View.VISIBLE
        savePersistedData()
        startTimer()
    }

    /** Configura o card com estilo customizado. */
    fun configureStyle(
        titleText: String? = null,
        @ColorInt titleColor: Int? = null,
        buttonTitle: String? = null,
        @ColorInt buttonColor: Int? = null,
    ) {
        titleText?.let { titleLabel.text = it }
        titleColor?.let {
            titleColorOverride = it
            titleLabel.setTextColor(it)
        }
        buttonTitle?.let { copyButton.text = it }
        buttonColor?.let {
            buttonColorOverride = it
            copyButton.setTextColor(it)
            copyButton.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = Color.TRANSPARENT,
                cornerRadiusDp = 25f,
                strokeColor = it,
                strokeWidthDp = 2f,
            )
        }
    }

    fun setCopyButtonVisible(visible: Boolean) {
        copyButton.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setResumeCardVisible(visible: Boolean) {
        resumeCard.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /** Expande o card (apenas se [collapsible]). */
    fun expand() { if (collapsible && !isExpanded) toggleExpanded() }

    /** Colapsa o card. */
    fun collapse() { if (collapsible && isExpanded) toggleExpanded() }

    fun hasPendingPix(): Boolean = prefs.getBoolean(KEY_IS_PENDING, false)

    fun clearData() {
        clearPersistedData()
        visibility = View.GONE
    }

    fun stopTimer() {
        handler.removeCallbacks(tick)
        targetTimestamp = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(tick)
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun toggleExpanded() {
        if (!collapsible) return
        isExpanded = !isExpanded
        updateExpandedState(animate = true)
    }

    private fun updateExpandedState(animate: Boolean) {
        if (!collapsible) return
        val show = isExpanded
        separator.visibility = if (show) View.VISIBLE else View.GONE
        resumeCard.visibility = if (show) View.VISIBLE else View.GONE
        copyButton.visibility = if (show) View.VISIBLE else View.GONE

        if (animate) {
            expandButton.animate().rotation(if (show) 180f else 0f).setDuration(300).start()
        } else {
            expandButton.rotation = if (show) 180f else 0f
        }
    }

    private fun refresh() {
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = cornerRadiusDp,
            strokeColor = DSSColors.borderDefault(),
            strokeWidthDp = 1f,
        )

        titleLabel.setTextColor(titleColorOverride ?: DSSColors.error())
        timeLabel.setTextColor(DSSColors.textPrimary())
        subtitleLabel.setTextColor(DSSColors.textSecondary())
        separator.setBackgroundColor(DSSColors.divider())

        val buttonColor = buttonColorOverride ?: DSSColors.error()
        copyButton.setTextColor(buttonColor)
        copyButton.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = Color.TRANSPARENT,
            cornerRadiusDp = 25f,
            strokeColor = buttonColor,
            strokeWidthDp = 2f,
        )
    }

    private fun startTimer() {
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun updateTimer() {
        val target = targetTimestamp ?: return
        val now = System.currentTimeMillis()
        val remaining = target - now
        if (remaining <= 0) {
            handler.removeCallbacks(tick)
            targetTimestamp = null
            timeLabel.text = "00:00:00"
            visibility = View.GONE
            clearPersistedData()
            onTimeout?.invoke()
        } else {
            val totalSeconds = remaining / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            timeLabel.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    private fun onCopyTapped() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Pix", pixCode ?: "Código PIX copiado"))

        val originalText = copyButton.text
        copyButton.text = "Pix copiado"
        copyButton.isEnabled = false
        handler.postDelayed({
            copyButton.text = originalText
            copyButton.isEnabled = true
        }, 2_000L)
    }

    private fun loadChevronDown(): Drawable? {
        val resId = resources.getIdentifier("ic_chevron_down", "drawable", context.packageName)
        return if (resId != 0) AppCompatResources.getDrawable(context, resId) else null
    }

    // MARK: Persistence

    private fun savePersistedData() {
        prefs.edit().apply {
            putBoolean(KEY_IS_PENDING, true)
            putLong(KEY_TARGET, targetTimestamp ?: 0L)
            putString(KEY_MSISDN, msisdn)
            putString(KEY_OFFER, offer)
            putInt(KEY_PRICE, priceInCents ?: 0)
            putString(KEY_PIX_CODE, pixCode)
            apply()
        }
    }

    private fun loadPersistedData() {
        if (!prefs.getBoolean(KEY_IS_PENDING, false)) return
        val savedTarget = prefs.getLong(KEY_TARGET, 0L)
        if (savedTarget <= System.currentTimeMillis()) {
            clearPersistedData()
            return
        }
        targetTimestamp = savedTarget
        msisdn = prefs.getString(KEY_MSISDN, null)
        offer = prefs.getString(KEY_OFFER, null)
        priceInCents = prefs.getInt(KEY_PRICE, 0)
        pixCode = prefs.getString(KEY_PIX_CODE, null)

        val m = msisdn
        val o = offer
        val p = priceInCents
        if (m != null && o != null && p != null) {
            resumeCard.configure(title = customTitle ?: "", number = m, offer = o, priceInCents = p)
            visibility = View.VISIBLE
            startTimer()
        } else {
            clearPersistedData()
        }
    }

    private fun clearPersistedData() {
        prefs.edit().clear().apply()
        targetTimestamp = null
        msisdn = null
        offer = null
        priceInCents = null
        pixCode = null
    }

    /**
     * Configura usando dados persistidos quando existirem.
     * Se já há dados persistidos válidos, mantém o estado.
     */
    fun configureWithPersistence(
        msisdn: String? = null,
        offer: String? = null,
        priceInCents: Int? = null,
        durationInSeconds: Int? = null,
        pixCode: String? = null,
    ) {
        if (hasPendingPix()) return
        if (msisdn == null || offer == null || priceInCents == null) return
        val duration = durationInSeconds ?: (6 * 60 * 60)
        if (duration <= 0) {
            visibility = View.GONE
            return
        }
        configure(
            msisdn = msisdn,
            offer = offer,
            priceInCents = priceInCents,
            durationInSeconds = duration,
            pixCode = pixCode,
        )
    }

    companion object {
        private const val KEY_IS_PENDING = "isPixPending"
        private const val KEY_TARGET = "targetDate"
        private const val KEY_MSISDN = "msisdn"
        private const val KEY_OFFER = "offer"
        private const val KEY_PRICE = "priceInCents"
        private const val KEY_PIX_CODE = "pixCode"

        @JvmStatic
        fun defaultStyle(context: Context, collapsible: Boolean = false): DSSPixPendentCardView {
            return DSSPixPendentCardView(context, collapsible = collapsible).apply {
                cornerRadiusDp = 12f
            }
        }
    }
}
