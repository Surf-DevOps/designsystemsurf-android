package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.viewpager2.widget.ViewPager2
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/** Status de portabilidade — equivalente ao enum Swift. */
enum class PortabilityStatus { CANCELLED, CONFIRMED, PENDING, SUCCESS, DEFAULT }

/**
 * Resultado da consulta de portabilidade — equivalente a `PortabilityResult` no iOS.
 *
 * @param descricaoTicketStatus mensagem detalhada (usada quando cancelado).
 * @param nuMsisdnOutraOperadora número da linha em portabilidade.
 * @param dtConfirmacao data de confirmação (string já formatada para exibição).
 */
data class PortabilityResult(
    val descricaoTicketStatus: String?,
    val nuMsisdnOutraOperadora: Long,
    val dtConfirmacao: String?,
)

/**
 * Resposta agregada da portabilidade — equivale ao `PortabilityStatusResponse`.
 */
data class PortabilityStatusResponse(
    val resultado: PortabilityResult?,
)

interface PortabilityCardDelegate {
    fun didTapTryAgainButton()
    fun didTapFinishButton()
    fun didUpdateToConfirmedOrPending(status: PortabilityStatus, response: PortabilityStatusResponse)
}

/**
 * Port do `PortabilityCardView` do iOS — view full-screen com título, subtítulo,
 * ilustração de progresso, área de card variável conforme o status (carrossel para
 * pending, single card pra confirmed/success, etc.) e botão de ação inferior.
 *
 * As imagens de progresso/ícones devem ser fornecidas pela app via [progressImages].
 */
class PortabilityCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Pacote de drawables que a app fornece para cada estado. */
    data class ProgressImages(
        val refused: Drawable? = null,
        val procedure: Drawable? = null,
        val completed: Drawable? = null,
        val pending: Drawable? = null,
        val confirmed: Drawable? = null,
    )

    var delegate: PortabilityCardDelegate? = null
    var progressImages: ProgressImages = ProgressImages()
        set(value) { field = value; refreshProgressImage() }

    private val scrollView = ScrollView(context).apply { isVerticalScrollBarEnabled = false }
    private val contentColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val titleLabel = TextView(context).apply {
        textSize = 24f
        typeface = DSSFont.bold(context, 24f).typeface
        setSingleLine(false)
    }
    private val subtitleLabel = TextView(context).apply {
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        setSingleLine(false)
    }
    private val progressImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private val cardContainer = FrameLayout(context)
    private val actionButton = AppCompatButton(context).apply {
        textSize = 16f
        typeface = DSSFont.medium(context, 16f).typeface
        isAllCaps = false
        visibility = View.GONE
        alpha = 0f
    }

    private var currentStatus: PortabilityStatus = PortabilityStatus.DEFAULT
    private var statusResponse: PortabilityStatusResponse? = null

    init {
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        scrollView.addView(
            contentColumn,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val hPad = 20f.dpToPx(context)
        contentColumn.setPadding(hPad, 20f.dpToPx(context), hPad, 32f.dpToPx(context))

        contentColumn.addView(titleLabel, columnLp(0))
        contentColumn.addView(subtitleLabel, columnLp(8f.dpToPx(context)))
        contentColumn.addView(
            progressImageView,
            LinearLayout.LayoutParams(
                315f.dpToPx(context),
                61f.dpToPx(context),
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 32f.dpToPx(context)
            },
        )
        contentColumn.addView(
            cardContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 32f.dpToPx(context) },
        )
        contentColumn.addView(
            actionButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50f.dpToPx(context),
            ).apply { topMargin = 32f.dpToPx(context) },
        )

        actionButton.setOnClickListener {
            when (currentStatus) {
                PortabilityStatus.CANCELLED -> delegate?.didTapTryAgainButton()
                PortabilityStatus.SUCCESS -> delegate?.didTapFinishButton()
                else -> Unit
            }
        }

        refresh()
        setupThemeObserver()
    }

    /**
     * Aplica o status recebido da API.
     * @param status string crua que vem da API (CANCELADO, SOLICITACAO_CONFIRMADA, etc).
     */
    fun configure(status: String, response: PortabilityStatusResponse) {
        val mapped = mapStatus(status)
        currentStatus = mapped
        statusResponse = response
        when (mapped) {
            PortabilityStatus.CANCELLED -> configError(response)
            PortabilityStatus.CONFIRMED -> {
                configConfirmed(response)
                delegate?.didUpdateToConfirmedOrPending(mapped, response)
            }
            PortabilityStatus.PENDING -> {
                configPending(response)
                delegate?.didUpdateToConfirmedOrPending(mapped, response)
            }
            PortabilityStatus.SUCCESS -> configSuccess(response)
            PortabilityStatus.DEFAULT -> configDefault(response)
        }
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackgroundColor(DSSColors.background())
        titleLabel.setTextColor(DSSColors.textPrimary())
        subtitleLabel.setTextColor(DSSColors.textSecondary())
        actionButton.setTextColor(DSSColors.buttonText())
        actionButton.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.primary(),
            cornerRadiusDp = 25f,
        )
    }

    // MARK: Status configs

    private fun configError(response: PortabilityStatusResponse) {
        titleLabel.text = "Infelizmente não conseguimos seguir com a sua portabilidade"
        subtitleLabel.text = "Verifique se:"
        progressImageView.setImageDrawable(progressImages.refused)
        progressImageView.visibility = View.GONE
        setupErrorCard(response)
        actionButton.text = "Tentar novamente"
        showActionButton()
    }

    private fun configPending(response: PortabilityStatusResponse) {
        titleLabel.text = "Falta pouco."
        subtitleLabel.text = "Confirme o SMS para seguir com a portabilidade"
        progressImageView.setImageDrawable(progressImages.procedure)
        progressImageView.visibility = View.VISIBLE
        setupCarousel(response)
        actionButton.visibility = View.GONE
        actionButton.alpha = 0f
    }

    private fun configConfirmed(response: PortabilityStatusResponse) {
        titleLabel.text = "Portabilidade em andamento"
        subtitleLabel.text = ""
        progressImageView.setImageDrawable(progressImages.procedure)
        progressImageView.visibility = View.VISIBLE
        setupSinglePending(response)
        actionButton.visibility = View.GONE
        actionButton.alpha = 0f
    }

    private fun configSuccess(response: PortabilityStatusResponse) {
        titleLabel.text = "Parabéns!"
        subtitleLabel.text =
            "Sua portabilidade foi realizada e agora você recebe GB bônus em toda recarga."
        progressImageView.setImageDrawable(progressImages.completed)
        progressImageView.visibility = View.VISIBLE
        setupSuccessCard(response)
        actionButton.text = "Finalizar"
        showActionButton()
    }

    private fun configDefault(response: PortabilityStatusResponse) {
        titleLabel.text = "Portabilidade em andamento"
        subtitleLabel.text = ""
        progressImageView.setImageDrawable(progressImages.procedure)
        progressImageView.visibility = View.VISIBLE
        setupSinglePending(response)
        actionButton.visibility = View.GONE
        actionButton.alpha = 0f
    }

    // MARK: Card sub-content

    private fun setupErrorCard(response: PortabilityStatusResponse) {
        clearCard()
        val pad = 16f.dpToPx(context)
        val msg = TextView(context).apply {
            text = response.resultado?.descricaoTicketStatus
                ?: "Não foi possível processar sua solicitação."
            textSize = 16f
            typeface = DSSFont.regular(context, 16f).typeface
            setSingleLine(false)
            setTextColor(DSSColors.textPrimary())
            setPadding(pad, pad, pad, pad)
        }
        cardContainer.background = cardBg()
        cardContainer.addView(
            msg,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun setupCarousel(response: PortabilityStatusResponse) {
        clearCard()
        cardContainer.background = cardBg()
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        val pager = ViewPager2(context)
        val pageControl = PageDots(context)

        val msisdn = formatPhoneNumber(response.resultado?.nuMsisdnOutraOperadora?.toString() ?: "")
        val pages = listOf(
            CarouselPage(
                icon = progressImages.pending,
                title = "Confirme o SMS",
                message = "Responda em até 24 horas o SMS que enviamos no número $msisdn " +
                    "para confirmar sua solicitação.",
            ),
            CarouselPage(
                icon = progressImages.pending,
                title = "Enquanto isso...",
                message = "Não se preocupe, os dois números continuarão funcionando até " +
                    "que a portabilidade seja concluída.",
            ),
        )
        pager.adapter = CarouselAdapter(pages)
        pager.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            180f.dpToPx(context),
        )
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { pageControl.setSelected(position) }
        })
        pageControl.setCount(pages.size)
        pageControl.setSelected(0)

        column.addView(
            pager,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                180f.dpToPx(context),
            ),
        )
        column.addView(
            pageControl,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 16f.dpToPx(context)
                bottomMargin = 16f.dpToPx(context)
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        cardContainer.addView(
            column,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun setupSinglePending(response: PortabilityStatusResponse) {
        clearCard()
        cardContainer.background = cardBg()
        val pad = 24f.dpToPx(context)
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
        }
        val icon = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(progressImages.confirmed)
        }
        val title = TextView(context).apply {
            text = "Previsão de conclusão\n${response.resultado?.dtConfirmacao ?: "Em breve"}"
            textSize = 16f
            typeface = DSSFont.medium(context, 16f).typeface
            gravity = Gravity.CENTER_HORIZONTAL
            setSingleLine(false)
            setTextColor(DSSColors.textPrimary())
        }
        val message = TextView(context).apply {
            text = "Não se preocupe, as duas linhas continuam funcionando normalmente até lá."
            textSize = 14f
            typeface = DSSFont.regular(context, 14f).typeface
            gravity = Gravity.CENTER_HORIZONTAL
            setSingleLine(false)
            setTextColor(DSSColors.textSecondary())
        }
        column.addView(
            icon,
            LinearLayout.LayoutParams(40f.dpToPx(context), 40f.dpToPx(context)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        column.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 16f.dpToPx(context) },
        )
        column.addView(
            message,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 12f.dpToPx(context) },
        )
        cardContainer.addView(
            column,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun setupSuccessCard(response: PortabilityStatusResponse) {
        clearCard()
        cardContainer.background = cardBg()
        val pad = 24f.dpToPx(context)
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
        }
        val phoneLabel = TextView(context).apply {
            text = "Agora seu número é:"
            textSize = 16f
            typeface = DSSFont.medium(context, 16f).typeface
            gravity = Gravity.CENTER_HORIZONTAL
            setSingleLine(false)
            setTextColor(DSSColors.textPrimary())
        }
        val numberLabel = TextView(context).apply {
            text = formatPhoneNumber(
                response.resultado?.nuMsisdnOutraOperadora?.toString() ?: "0",
            )
            textSize = 20f
            typeface = DSSFont.bold(context, 20f).typeface
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(DSSColors.primary())
        }
        column.addView(
            phoneLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        column.addView(
            numberLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 16f.dpToPx(context) },
        )
        cardContainer.addView(
            column,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun clearCard() {
        cardContainer.removeAllViews()
    }

    private fun refreshProgressImage() {
        // re-evaluate current state in case images were set after configure
        statusResponse?.let { configure(currentStatus.name, it) }
    }

    private fun showActionButton() {
        actionButton.visibility = View.VISIBLE
        actionButton.animate().alpha(1f).setStartDelay(300).setDuration(500).start()
    }

    private fun cardBg() = DrawableFactory.rounded(
        context = context,
        backgroundColor = DSSColors.surface(),
        cornerRadiusDp = 16f,
        strokeColor = DSSColors.borderDefault(),
        strokeWidthDp = 0.5f,
    )

    private fun columnLp(top: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = top }

    private fun mapStatus(raw: String): PortabilityStatus = when (raw.uppercase()) {
        "CANCELADO", "CONFLITO" -> PortabilityStatus.CANCELLED
        "SOLICITACAO_CONFIRMADA", "PRE_REGISTRADO" -> PortabilityStatus.CONFIRMED
        "PENDENTE" -> PortabilityStatus.PENDING
        "SUCESSO" -> PortabilityStatus.SUCCESS
        else -> PortabilityStatus.DEFAULT
    }

    private fun formatPhoneNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.length >= 11) {
            val ddd = digits.substring(0, 2)
            val first = digits.substring(2, 3)
            val middle = digits.substring(3, 7)
            val last = digits.substring(7, 11)
            return "($ddd) $first $middle-$last"
        }
        return raw
    }

    // MARK: Carousel page model + adapter + dots

    private data class CarouselPage(
        val icon: Drawable?,
        val title: String,
        val message: String,
    )

    private inner class CarouselAdapter(
        private val pages: List<CarouselPage>,
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<CarouselAdapter.VH>() {

        inner class VH(val root: LinearLayout) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(root) {
            val icon: ImageView = root.findViewWithTag("icon")
            val title: TextView = root.findViewWithTag("title")
            val message: TextView = root.findViewWithTag("message")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val pad = 16f.dpToPx(ctx)
            val root = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(pad, pad, pad, pad)
                background = DrawableFactory.rounded(
                    context = ctx,
                    backgroundColor = DSSColors.surface(),
                    cornerRadiusDp = 16f,
                    strokeColor = DSSColors.borderDefault(),
                    strokeWidthDp = 0.5f,
                )
                layoutParams = androidx.recyclerview.widget.RecyclerView.LayoutParams(
                    androidx.recyclerview.widget.RecyclerView.LayoutParams.MATCH_PARENT,
                    androidx.recyclerview.widget.RecyclerView.LayoutParams.MATCH_PARENT,
                )
            }
            val icon = ImageView(ctx).apply {
                tag = "icon"
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            val title = TextView(ctx).apply {
                tag = "title"
                textSize = 18f
                typeface = DSSFont.medium(ctx, 18f).typeface
                gravity = Gravity.CENTER_HORIZONTAL
                setSingleLine(false)
                setTextColor(DSSColors.textPrimary())
            }
            val message = TextView(ctx).apply {
                tag = "message"
                textSize = 14f
                typeface = DSSFont.regular(ctx, 14f).typeface
                gravity = Gravity.CENTER_HORIZONTAL
                setSingleLine(false)
                setTextColor(DSSColors.textSecondary())
            }
            root.addView(
                icon,
                LinearLayout.LayoutParams(40f.dpToPx(ctx), 40f.dpToPx(ctx)),
            )
            root.addView(
                title,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 16f.dpToPx(ctx) },
            )
            root.addView(
                message,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 12f.dpToPx(ctx) },
            )
            return VH(root)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = pages[position]
            holder.icon.setImageDrawable(p.icon)
            holder.title.text = p.title
            holder.message.text = p.message
        }

        override fun getItemCount(): Int = pages.size
    }

    /** Simples conjunto de dots para acompanhar o pager. */
    private class PageDots(context: Context) : LinearLayout(context) {
        init { orientation = HORIZONTAL; gravity = Gravity.CENTER }
        private val dots = mutableListOf<View>()
        fun setCount(n: Int) {
            removeAllViews()
            dots.clear()
            val size = 8f.dpToPx(context)
            for (i in 0 until n) {
                val v = View(context)
                v.layoutParams = LayoutParams(size, size).apply {
                    leftMargin = 4f.dpToPx(context)
                    rightMargin = 4f.dpToPx(context)
                }
                v.background = DrawableFactory.rounded(
                    context = context,
                    backgroundColor = DSSColors.borderDefault(),
                    cornerRadiusDp = 4f,
                )
                dots.add(v)
                addView(v)
            }
        }

        fun setSelected(index: Int) {
            dots.forEachIndexed { i, v ->
                v.background = DrawableFactory.rounded(
                    context = context,
                    backgroundColor = if (i == index) DSSColors.textPrimary()
                    else DSSColors.borderDefault(),
                    cornerRadiusDp = 4f,
                )
            }
        }
    }
}
