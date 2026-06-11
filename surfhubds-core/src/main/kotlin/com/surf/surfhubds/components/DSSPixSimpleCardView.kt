package com.surf.surfhubds.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
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
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import java.util.concurrent.TimeUnit

/**
 * Port do `DSSPixSimpleCardView` do iOS — card "simples" (não collapsible) para mostrar
 * uma recarga PIX pendente com timer regressivo e botão de copiar código.
 *
 * Persiste estado em [SharedPreferences] (prefix `DSSPixSimpleCard_*`) para reaparecer
 * em sessões subsequentes enquanto o timer não expirar.
 */
class DSSPixSimpleCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Disparado quando o timer chega a zero. */
    var onTimeout: (() -> Unit)? = null

    /** Disparado ao tocar no card. */
    var onCardTapped: (() -> Unit)? = null

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
        textSize = 14f
        typeface = DSSFont.medium(context, 14f).typeface
        maxLines = 1
    }
    private val timeLabel = TextView(context).apply {
        text = "00:00:00"
        textSize = 14f
        typeface = DSSFont.medium(context, 14f).typeface
        maxLines = 1
        gravity = Gravity.START
    }

    private val resumeCard = DSSResumeCard(context).apply {
        setCategoryLabels(number = "Número", offer = "Oferta", price = "Valor")
    }

    private val copyButton = AppCompatButton(context).apply {
        text = "Copiar código"
        textSize = 16f
        typeface = DSSFont.medium(context, 16f).typeface
        isAllCaps = false
    }

    /** Pix icon — opcionalmente injetado pela app (ImageLoader nativo). */
    var pixIcon: android.graphics.drawable.Drawable? = null
        set(value) { field = value; iconImageView.setImageDrawable(value) }

    var cornerRadiusDp: Float = 12f
        set(value) { field = value; refresh() }

    private var title: String? = null
    private var msisdn: String? = null
    private var offer: String? = null
    private var priceInCents: Int? = null
    private var pixCode: String? = null

    // Overrides de estilo (configureStyle). iOS preserva essas cores em troca de tema.
    @ColorInt private var titleColorOverride: Int? = null
    @ColorInt private var buttonColorOverride: Int? = null

    private var targetTimestamp: Long? = null
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateTimer()
            if (targetTimestamp != null) handler.postDelayed(this, 1_000L)
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.applicationContext.getSharedPreferences("DSSPixSimpleCard", Context.MODE_PRIVATE)
    }

    init {
        visibility = View.GONE

        addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
        val pad = 16f.dpToPx(context)
        container.setPadding(pad, pad, pad, pad)

        // Header row
        headerRow.addView(
            iconImageView,
            LinearLayout.LayoutParams(24f.dpToPx(context), 24f.dpToPx(context)).apply {
                marginEnd = 8f.dpToPx(context)
            },
        )
        headerRow.addView(
            titleLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        container.addView(
            headerRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        container.addView(
            timeLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 8f.dpToPx(context) },
        )
        container.addView(
            resumeCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        container.addView(
            copyButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50f.dpToPx(context),
            ).apply { topMargin = 20f.dpToPx(context) },
        )

        container.setOnClickListener { onCardTapped?.invoke() }
        copyButton.setOnClickListener { onCopyTapped() }

        // Default pix icon — pode ser sobrescrito via [pixIcon].
        if (pixIcon == null) iconImageView.setImageDrawable(PaymentMethodImages.pixIcon(context))

        refresh()
        setupThemeObserver()
        loadPersistedData()
    }

    /**
     * Configura o card com os dados do PIX pendente.
     * @param durationInSeconds duração total do timer em segundos.
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
        this.title = title
        this.msisdn = msisdn
        this.offer = offer
        this.priceInCents = priceInCents
        this.pixCode = pixCode

        resumeCard.configure(title = title, number = msisdn, offer = offer, priceInCents = priceInCents)

        targetTimestamp = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(durationInSeconds.toLong())

        visibility = View.VISIBLE
        savePersistedData()
        startTimer()
    }

    /**
     * Configura o card com estilo customizado (espelha `configureStyle` do iOS).
     * Cada parâmetro nulo é ignorado, preservando o valor atual.
     * @param titleColor cor do título (ColorInt).
     * @param buttonColor cor do texto e da borda do botão de copiar (ColorInt).
     */
    fun configureStyle(
        titleText: String? = null,
        @ColorInt titleColor: Int? = null,
        buttonTitle: String? = null,
        @ColorInt buttonColor: Int? = null,
    ) {
        if (titleText != null) titleLabel.text = titleText
        if (titleColor != null) {
            titleColorOverride = titleColor
            titleLabel.setTextColor(titleColor)
        }
        if (buttonTitle != null) copyButton.text = buttonTitle
        if (buttonColor != null) {
            buttonColorOverride = buttonColor
            copyButton.setTextColor(buttonColor)
            copyButton.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = android.graphics.Color.TRANSPARENT,
                cornerRadiusDp = 25f,
                strokeColor = buttonColor,
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

    /** Verifica se existem dados persistidos. */
    fun hasPendingPix(): Boolean = prefs.getBoolean(KEY_IS_PENDING, false)

    /** Limpa dados persistidos manualmente. */
    fun clearData() {
        clearPersistedData()
    }

    /** Para o timer (quando a view é desanexada). iOS apenas invalida o timer, mantém o targetDate. */
    fun stopTimer() {
        handler.removeCallbacks(tick)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(tick)
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        // iOS applyContainerColors():
        //  .black  -> backgroundColor secundário, sem borda
        //  .dark   -> backgroundColor secundário, borda 2pt (cinza)
        //  .light  -> backgroundColor padrão, sem borda
        val scheme = ThemeManager.colorScheme
        val strokeColor: Int?
        val strokeWidthDp: Float
        if (scheme == ColorScheme.DARK) {
            strokeColor = DSSColors.borderDefault()
            strokeWidthDp = 2f
        } else {
            strokeColor = null
            strokeWidthDp = 0f
        }
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = cornerRadiusDp,
            strokeColor = strokeColor,
            strokeWidthDp = strokeWidthDp,
        )
        // Título: vermelho-escuro literal do iOS UIColor(red:0.65, green:0.16, blue:0.16) = #A62929
        // (cor literal, não semântica). Override de configureStyle tem prioridade.
        titleLabel.setTextColor(titleColorOverride ?: PIX_DARK_RED)
        timeLabel.setTextColor(DSSColors.textPrimary())

        val buttonColor = buttonColorOverride ?: PIX_DARK_RED
        copyButton.setTextColor(buttonColor)
        copyButton.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = android.graphics.Color.TRANSPARENT,
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
            // iOS reconfigura com title vazio ("") ao restaurar dados persistidos.
            resumeCard.configure(title = "", number = m, offer = o, priceInCents = p)
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
     * Se já há dados persistidos válidos, não faz nada — mantém o estado.
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
        // iOS UIColor(red: 0.65, green: 0.16, blue: 0.16, alpha: 1.0) — literal hardcoded, não token.
        @ColorInt private val PIX_DARK_RED: Int = android.graphics.Color.rgb(166, 41, 41)

        private const val KEY_IS_PENDING = "DSSPixSimpleCard_isPixPending"
        private const val KEY_TARGET = "DSSPixSimpleCard_targetDate"
        private const val KEY_MSISDN = "DSSPixSimpleCard_msisdn"
        private const val KEY_OFFER = "DSSPixSimpleCard_offer"
        private const val KEY_PRICE = "DSSPixSimpleCard_priceInCents"
        private const val KEY_PIX_CODE = "DSSPixSimpleCard_pixCode"

        @JvmStatic
        fun defaultStyle(context: Context): DSSPixSimpleCardView =
            DSSPixSimpleCardView(context).apply { cornerRadiusDp = 12f }
    }
}
