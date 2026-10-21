package com.surf.surfhubds.components

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.R
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Pop-up de avaliação do app (CSAT 1 a 5 estrelas), Figma NOVO-APP-SURF "Jornada de
 * Avaliação": sobe por cima da tela "recarga realizada".
 *
 * Estados:
 * 1. **Nota**: estrelas vazias, "Enviar avaliação" desabilitado e "Agora não". Ao tocar
 *    numa estrela aparecem o rótulo da nota, a pergunta, o comentário (opcional, 10 a 300
 *    caracteres) e as categorias do grupo da nota (múltipla escolha, opcionais).
 * 2. **Agradecimento**: notas 1 a 3 → "Recebido. Vamos trabalhar nisso." com "Fechar";
 *    notas 4 e 5 → "Obrigado pelas N estrelas!" com "Avaliar na loja" ou "Agora não".
 *
 * O componente não faz rede: [Delegate.ratingSheetDidSubmit] entrega a nota e quem usa
 * chama [showThanks] (sucesso) ou [showSubmitError] (falha, permite reenviar).
 *
 * Fechar sem responder (toque fora, swipe ou "Agora não" no estado de nota) dispara
 * [Delegate.ratingSheetDidDismissWithoutAnswer]; fechar depois do envio dispara
 * [Delegate.ratingSheetDidFinish].
 */
class DSSRatingBottomSheet : BottomSheetDialogFragment() {

    data class Category(val id: Int, val title: String)

    data class Submission(val rating: Int, val categoryIds: List<Int>, val comment: String?)

    interface Delegate {
        /** "Enviar avaliação". O sheet fica em envio até [showThanks] ou [showSubmitError]. */
        fun ratingSheetDidSubmit(sheet: DSSRatingBottomSheet, submission: Submission) {}

        /** "Avaliar na loja" (notas 4 e 5). O sheet fecha em seguida. */
        fun ratingSheetDidTapRateInStore(sheet: DSSRatingBottomSheet) {}

        /** Fechado antes de enviar: toque fora, swipe ou "Agora não". */
        fun ratingSheetDidDismissWithoutAnswer(sheet: DSSRatingBottomSheet) {}

        /** Fechado depois do envio ("Fechar", "Agora não", "Avaliar na loja", toque fora ou swipe). */
        fun ratingSheetDidFinish(sheet: DSSRatingBottomSheet) {}
    }

    private enum class Stage { RATING, THANKS }

    var delegate: Delegate? = null

    /** Categorias das notas 1 a 3. */
    var negativeCategories: List<Category> = emptyList()

    /** Categorias das notas 4 e 5. */
    var positiveCategories: List<Category> = emptyList()

    private var stage = Stage.RATING
    private var rating = 0
    private val selectedCategoryIds = linkedSetOf<Int>()
    private var isSubmitting = false
    private var didFireDismiss = false

    private lateinit var ratingContent: LinearLayout
    private lateinit var initialHeader: LinearLayout
    private lateinit var starsRow: LinearLayout
    private lateinit var ratingLabel: TextView
    private lateinit var questionLabel: TextView
    private lateinit var commentBox: LinearLayout
    private lateinit var commentField: EditText
    private lateinit var commentHintLabel: TextView
    private lateinit var commentCounterLabel: TextView
    private lateinit var categoriesFlow: FlowLayout
    private lateinit var errorLabel: TextView
    private lateinit var sendButton: DSSPrincipalButton
    private lateinit var notNowLink: TextView

    private lateinit var thanksContent: LinearLayout
    private lateinit var thanksBadge: DSSIconBadge
    private lateinit var thanksStarsRow: LinearLayout
    private lateinit var thanksTitle: TextView
    private lateinit var thanksMessage: TextView
    private lateinit var storeButton: DSSPrincipalButton
    private lateinit var thanksLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recriado pelo sistema (rotação, processo restaurado) chega sem delegate nem
        // categorias: fecha em silêncio e quem usa reabre pela pendência que guardou.
        if (savedInstanceState != null && delegate == null) {
            didFireDismiss = true
            dismissAllowingStateLoss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        // O teclado do comentário empurra o sheet em vez de cobrir o botão.
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet,
            ) ?: return@setOnShowListener
            sheet.setBackgroundColor(Color.TRANSPARENT)
            BottomSheetBehavior.from(sheet).apply {
                isFitToContents = true
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        val scroll = NestedScrollView(ctx).apply {
            background = GradientDrawable().apply {
                val r = 24f.dpToPx(ctx).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setColor(DSSColors.surface())
            }
        }
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24f.dpToPx(ctx), 12f.dpToPx(ctx), 24f.dpToPx(ctx), 24f.dpToPx(ctx))
        }
        val handle = View(ctx).apply {
            background = DrawableFactory.rounded(
                ctx, ColorUtils.setAlphaComponent(DSSColors.textTertiary(), 0x66), 2f,
            )
        }
        root.addView(handle, LinearLayout.LayoutParams(40f.dpToPx(ctx), 4f.dpToPx(ctx)))

        ratingContent = buildRatingContent(ctx)
        root.addView(ratingContent, matchWidth().apply { topMargin = 20f.dpToPx(ctx) })
        thanksContent = buildThanksContent(ctx)
        root.addView(thanksContent, matchWidth().apply { topMargin = 20f.dpToPx(ctx) })

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        render()
        return scroll
    }

    // region Construção

    private fun buildRatingContent(ctx: Context): LinearLayout {
        val content = vertical(ctx)

        initialHeader = vertical(ctx)
        initialHeader.addView(label(ctx, 18f, bold = true).apply {
            text = AppStrings.brand(ctx, "rating_title", TITLE)
            setTextColor(DSSColors.textPrimary())
        }, matchWidth())
        initialHeader.addView(label(ctx, 13f).apply {
            text = AppStrings.brand(ctx, "rating_subtitle", SUBTITLE)
            setTextColor(DSSColors.textSecondary())
        }, matchWidth().apply { topMargin = 8f.dpToPx(ctx) })
        content.addView(initialHeader, matchWidth().apply { bottomMargin = 20f.dpToPx(ctx) })

        starsRow = starsRow(ctx, STAR_SIZE_DP, STAR_SPACING_DP, interactive = true)
        content.addView(starsRow, wrap())

        ratingLabel = label(ctx, 16f, bold = true)
        content.addView(ratingLabel, matchWidth().apply { topMargin = 10f.dpToPx(ctx) })

        questionLabel = label(ctx, 13f).apply { setTextColor(DSSColors.textSecondary()) }
        content.addView(questionLabel, matchWidth().apply {
            topMargin = 8f.dpToPx(ctx)
            marginStart = 12f.dpToPx(ctx)
            marginEnd = 12f.dpToPx(ctx)
        })

        commentBox = buildCommentBox(ctx)
        content.addView(commentBox, matchWidth().apply { topMargin = 16f.dpToPx(ctx) })

        categoriesFlow = FlowLayout(ctx, 8f.dpToPx(ctx), 8f.dpToPx(ctx))
        content.addView(categoriesFlow, matchWidth().apply { topMargin = 16f.dpToPx(ctx) })

        errorLabel = label(ctx, 12f).apply { setTextColor(DSSColors.error()) }
        content.addView(errorLabel, matchWidth().apply { topMargin = 12f.dpToPx(ctx) })

        sendButton = DSSPrincipalButton(ctx).apply {
            text = AppStrings.brand(ctx, "rating_send", SEND)
            applyPillStyle(44f, 15f)
            customBackgroundColor = DSSColors.primaryButton()
            customTextColor = DSSColors.buttonText()
            onTap = { submit() }
        }
        content.addView(sendButton, matchWidth().apply {
            topMargin = 20f.dpToPx(ctx)
            marginStart = 12f.dpToPx(ctx)
            marginEnd = 12f.dpToPx(ctx)
        })

        notNowLink = link(ctx).apply {
            text = AppStrings.brand(ctx, "rating_not_now", NOT_NOW)
            setOnClickListener { dismiss() }
        }
        content.addView(notNowLink, wrap().apply { topMargin = 8f.dpToPx(ctx) })
        return content
    }

    private fun buildCommentBox(ctx: Context): LinearLayout {
        val box = vertical(ctx).apply {
            gravity = Gravity.NO_GRAVITY
            background = DrawableFactory.rounded(
                ctx, DSSColors.backgroundSecondary(), 12f, DSSColors.borderSubtle(), 1f,
            )
            val pad = 12f.dpToPx(ctx)
            setPadding(pad, pad, pad, 8f.dpToPx(ctx))
        }
        commentField = EditText(ctx).apply {
            background = null
            setPadding(0, 0, 0, 0)
            gravity = Gravity.TOP or Gravity.START
            minLines = 3
            maxLines = 6
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(COMMENT_MAX))
            textSize = 13f
            typeface = DSSFont.regular(ctx, 13f).typeface
            hint = AppStrings.brand(ctx, "rating_comment_placeholder", COMMENT_PLACEHOLDER)
            setTextColor(DSSColors.textPrimary())
            setHintTextColor(DSSColors.textTertiary())
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    errorLabel.isVisible = false
                    renderCommentFooter()
                    renderSendButton()
                }
            })
        }
        box.addView(commentField, matchWidth())

        val footer = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        commentHintLabel = TextView(ctx).apply {
            textSize = 11f
            typeface = DSSFont.regular(ctx, 11f).typeface
        }
        commentCounterLabel = TextView(ctx).apply {
            textSize = 11f
            typeface = DSSFont.regular(ctx, 11f).typeface
            gravity = Gravity.END
        }
        footer.addView(commentHintLabel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        footer.addView(commentCounterLabel, wrap())
        box.addView(footer, matchWidth().apply { topMargin = 8f.dpToPx(ctx) })
        return box
    }

    private fun buildThanksContent(ctx: Context): LinearLayout {
        val content = vertical(ctx)

        thanksBadge = DSSIconBadge(ctx).apply { autoPadding = true }
        content.addView(thanksBadge, LinearLayout.LayoutParams(48f.dpToPx(ctx), 48f.dpToPx(ctx)))

        thanksStarsRow = starsRow(ctx, THANKS_STAR_SIZE_DP, 4f, interactive = false)
        content.addView(thanksStarsRow, wrap().apply { topMargin = 16f.dpToPx(ctx) })

        thanksTitle = label(ctx, 18f, bold = true).apply { setTextColor(DSSColors.textPrimary()) }
        content.addView(thanksTitle, matchWidth().apply { topMargin = 16f.dpToPx(ctx) })

        thanksMessage = label(ctx, 13f).apply { setTextColor(DSSColors.textSecondary()) }
        content.addView(thanksMessage, matchWidth().apply {
            topMargin = 10f.dpToPx(ctx)
            marginStart = 16f.dpToPx(ctx)
            marginEnd = 16f.dpToPx(ctx)
        })

        storeButton = DSSPrincipalButton(ctx).apply {
            text = AppStrings.brand(ctx, "rating_store", RATE_IN_STORE)
            applyPillStyle(44f, 15f)
            customBackgroundColor = DSSColors.primaryButton()
            customTextColor = DSSColors.buttonText()
            onTap = {
                delegate?.ratingSheetDidTapRateInStore(this@DSSRatingBottomSheet)
                dismiss()
            }
        }
        content.addView(storeButton, matchWidth().apply {
            topMargin = 24f.dpToPx(ctx)
            marginStart = 12f.dpToPx(ctx)
            marginEnd = 12f.dpToPx(ctx)
        })

        thanksLink = link(ctx).apply { setOnClickListener { dismiss() } }
        content.addView(thanksLink, wrap().apply { topMargin = 8f.dpToPx(ctx) })
        return content
    }

    private fun starsRow(ctx: Context, sizeDp: Float, spacingDp: Float, interactive: Boolean): LinearLayout {
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        for (value in 1..MAX_RATING) {
            val star = ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = AppStrings.brand(ctx, "rating_star_description", STAR_DESCRIPTION, value)
                if (interactive) setOnClickListener { selectRating(value) }
            }
            row.addView(star, LinearLayout.LayoutParams(sizeDp.dpToPx(ctx), sizeDp.dpToPx(ctx)).apply {
                if (value > 1) marginStart = spacingDp.dpToPx(ctx)
            })
        }
        return row
    }

    // endregion

    // region Estado

    /** Envio confirmado: troca para o agradecimento da nota enviada. */
    fun showThanks() {
        isSubmitting = false
        stage = Stage.THANKS
        isCancelable = true
        if (view != null) render()
    }

    /** Envio falhou: volta ao formulário com a mensagem (ou a padrão) e permite reenviar. */
    fun showSubmitError(message: String? = null) {
        isSubmitting = false
        isCancelable = true
        if (view == null) return
        errorLabel.text = message ?: AppStrings.brand(requireContext(), "rating_submit_error", SUBMIT_ERROR)
        errorLabel.isVisible = true
        renderSendButton()
    }

    private fun selectRating(value: Int) {
        if (isSubmitting) return
        val previousGroupPositive = rating >= POSITIVE_FROM
        rating = value
        // As categorias mudam de grupo entre 3 e 4 estrelas: a seleção do outro grupo não vale.
        if (previousGroupPositive != (value >= POSITIVE_FROM)) selectedCategoryIds.clear()
        errorLabel.isVisible = false
        render()
    }

    private fun submit() {
        if (!canSubmit()) return
        isSubmitting = true
        // Durante o envio o toque fora não pode marcar a avaliação como dispensada.
        isCancelable = false
        errorLabel.isVisible = false
        renderSendButton()
        val comment = commentField.text.toString().trim().ifEmpty { null }
        delegate?.ratingSheetDidSubmit(
            this,
            Submission(rating, selectedCategoryIds.toList(), comment),
        )
    }

    private fun canSubmit(): Boolean {
        if (rating == 0 || isSubmitting) return false
        val length = commentField.text.toString().trim().length
        return length == 0 || length >= COMMENT_MIN
    }

    private fun render() {
        ratingContent.isVisible = stage == Stage.RATING
        thanksContent.isVisible = stage == Stage.THANKS
        if (stage == Stage.RATING) renderRating() else renderThanks()
    }

    private fun renderRating() {
        val ctx = requireContext()
        val rated = rating > 0
        initialHeader.isVisible = !rated
        notNowLink.isVisible = !rated
        ratingLabel.isVisible = rated
        questionLabel.isVisible = rated
        commentBox.isVisible = rated
        renderStars(starsRow, rating)
        if (rated) {
            ratingLabel.text = ratingTitle(ctx, rating)
            ratingLabel.setTextColor(ratingColor(rating))
            questionLabel.text = ratingQuestion(ctx, rating)
            renderCommentFooter()
        }
        renderCategories()
        renderSendButton()
    }

    private fun renderCategories() {
        val ctx = requireContext()
        categoriesFlow.removeAllViews()
        if (rating == 0) {
            categoriesFlow.isVisible = false
            return
        }
        val categories = if (rating >= POSITIVE_FROM) positiveCategories else negativeCategories
        categoriesFlow.isVisible = categories.isNotEmpty()
        categories.forEach { category ->
            val chip = TextView(ctx).apply {
                text = category.title
                textSize = 12f
                typeface = DSSFont.regular(ctx, 12f).typeface
                gravity = Gravity.CENTER
                maxLines = 1
                val padH = 12f.dpToPx(ctx)
                val padV = 6f.dpToPx(ctx)
                setPadding(padH, padV, padH, padV)
                setOnClickListener {
                    if (isSubmitting) return@setOnClickListener
                    if (!selectedCategoryIds.remove(category.id)) selectedCategoryIds.add(category.id)
                    styleChip(this, category.id in selectedCategoryIds)
                }
            }
            styleChip(chip, category.id in selectedCategoryIds)
            categoriesFlow.addView(chip)
        }
    }

    private fun styleChip(chip: TextView, selected: Boolean) {
        val ctx = chip.context
        chip.isSelected = selected
        chip.background = if (selected) {
            DrawableFactory.rounded(ctx, DSSColors.primaryButton(), CHIP_RADIUS_DP)
        } else {
            DrawableFactory.rounded(ctx, DSSColors.surface(), CHIP_RADIUS_DP, DSSColors.borderSubtle(), 1f)
        }
        chip.setTextColor(if (selected) DSSColors.buttonText() else DSSColors.textSecondary())
    }

    private fun renderCommentFooter() {
        val ctx = requireContext()
        val length = commentField.text.toString().trim().length
        val tooShort = length in 1 until COMMENT_MIN
        commentHintLabel.text = if (tooShort) {
            AppStrings.brand(ctx, "rating_comment_min_format", COMMENT_MIN_FORMAT, COMMENT_MIN)
        } else {
            AppStrings.brand(ctx, "rating_comment_optional", COMMENT_OPTIONAL)
        }
        commentHintLabel.setTextColor(if (tooShort) DSSColors.error() else DSSColors.textTertiary())
        commentCounterLabel.text = "${commentField.text.length}/$COMMENT_MAX"
        commentCounterLabel.setTextColor(DSSColors.textTertiary())
    }

    private fun renderSendButton() {
        val ctx = requireContext()
        val enabled = canSubmit()
        sendButton.isEnabled = enabled
        sendButton.alpha = if (enabled || isSubmitting) 1f else DISABLED_ALPHA
        sendButton.text = if (isSubmitting) {
            AppStrings.brand(ctx, "rating_sending", SENDING)
        } else {
            AppStrings.brand(ctx, "rating_send", SEND)
        }
    }

    private fun renderThanks() {
        val ctx = requireContext()
        val positive = rating >= POSITIVE_FROM
        thanksBadge.configure(
            ContextCompat.getDrawable(ctx, R.drawable.dss_ic_chat),
            if (positive) DSSIconBadge.Style.SUCCESS else DSSIconBadge.Style.ERROR,
        )
        thanksStarsRow.isVisible = positive
        renderStars(thanksStarsRow, rating)
        storeButton.isVisible = positive
        if (positive) {
            thanksTitle.text = AppStrings.brand(ctx, "rating_thanks_positive_title_format", THANKS_POSITIVE_TITLE_FORMAT, rating)
            thanksMessage.text = AppStrings.brand(ctx, "rating_thanks_positive_message", THANKS_POSITIVE_MESSAGE)
            thanksLink.text = AppStrings.brand(ctx, "rating_not_now", NOT_NOW)
        } else {
            thanksTitle.text = AppStrings.brand(ctx, "rating_thanks_negative_title", THANKS_NEGATIVE_TITLE)
            thanksMessage.text = AppStrings.brand(ctx, "rating_thanks_negative_message", THANKS_NEGATIVE_MESSAGE)
            thanksLink.text = AppStrings.brand(ctx, "rating_close", CLOSE)
        }
    }

    private fun renderStars(row: LinearLayout, filledCount: Int) {
        val ctx = row.context
        for (index in 0 until row.childCount) {
            val star = row.getChildAt(index) as ImageView
            val filled = index < filledCount
            star.setImageResource(if (filled) R.drawable.dss_ic_star_filled else R.drawable.dss_ic_star_outline)
            star.imageTintList = ColorStateList.valueOf(if (filled) STAR_COLOR else DSSColors.textTertiary())
            star.isSelected = filled
        }
        row.contentDescription = if (filledCount > 0) {
            AppStrings.brand(ctx, "rating_selected_description", SELECTED_DESCRIPTION, filledCount)
        } else {
            null
        }
    }

    private fun ratingTitle(ctx: Context, value: Int): String = when (value) {
        1 -> AppStrings.brand(ctx, "rating_label_1", "Péssimo")
        2 -> AppStrings.brand(ctx, "rating_label_2", "Ruim")
        3 -> AppStrings.brand(ctx, "rating_label_3", "Pode melhorar")
        4 -> AppStrings.brand(ctx, "rating_label_4", "Muito bom")
        else -> AppStrings.brand(ctx, "rating_label_5", "Excelente")
    }

    private fun ratingQuestion(ctx: Context, value: Int): String = when (value) {
        1, 2 -> AppStrings.brand(ctx, "rating_question_low", QUESTION_LOW)
        3 -> AppStrings.brand(ctx, "rating_question_medium", QUESTION_MEDIUM)
        else -> AppStrings.brand(ctx, "rating_question_high", QUESTION_HIGH)
    }

    private fun ratingColor(value: Int): Int = when (value) {
        1, 2 -> DSSColors.error()
        3 -> MEDIUM_COLOR
        else -> DSSColors.success()
    }

    // endregion

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        fireDismissOnce()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        fireDismissOnce()
    }

    private fun fireDismissOnce() {
        if (didFireDismiss) return
        didFireDismiss = true
        if (stage == Stage.THANKS) {
            delegate?.ratingSheetDidFinish(this)
        } else {
            delegate?.ratingSheetDidDismissWithoutAnswer(this)
        }
    }

    // region Helpers de view

    private fun vertical(ctx: Context) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun label(ctx: Context, sizeSp: Float, bold: Boolean = false) = TextView(ctx).apply {
        gravity = Gravity.CENTER
        textSize = sizeSp
        typeface = if (bold) DSSFont.bold(ctx, sizeSp).typeface else DSSFont.regular(ctx, sizeSp).typeface
    }

    private fun link(ctx: Context) = TextView(ctx).apply {
        gravity = Gravity.CENTER
        textSize = 13f
        typeface = DSSFont.regular(ctx, 13f).typeface
        setTextColor(DSSColors.textPrimary())
        val pad = 8f.dpToPx(ctx)
        setPadding(pad * 2, pad, pad * 2, pad)
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    private fun wrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    /** Quebra os filhos em linhas centralizadas (chips das categorias). */
    private class FlowLayout(
        context: Context,
        private val horizontalSpacing: Int,
        private val verticalSpacing: Int,
    ) : ViewGroup(context) {

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val maxWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            val childSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
            var lineWidth = 0
            var lineHeight = 0
            var totalHeight = 0
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == GONE) continue
                child.measure(childSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
                val needed = if (lineWidth == 0) child.measuredWidth else lineWidth + horizontalSpacing + child.measuredWidth
                if (needed > maxWidth && lineWidth > 0) {
                    totalHeight += lineHeight + verticalSpacing
                    lineWidth = child.measuredWidth
                    lineHeight = child.measuredHeight
                } else {
                    lineWidth = needed
                    lineHeight = maxOf(lineHeight, child.measuredHeight)
                }
            }
            totalHeight += lineHeight
            setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                resolveSize(totalHeight + paddingTop + paddingBottom, heightMeasureSpec),
            )
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            val maxWidth = r - l - paddingLeft - paddingRight
            var top = paddingTop
            var line = mutableListOf<View>()
            var lineWidth = 0

            fun layoutLine() {
                if (line.isEmpty()) return
                var left = paddingLeft + (maxWidth - lineWidth) / 2
                val lineHeight = line.maxOf { it.measuredHeight }
                line.forEach { child ->
                    child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)
                    left += child.measuredWidth + horizontalSpacing
                }
                top += lineHeight + verticalSpacing
            }

            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == GONE) continue
                val needed = if (lineWidth == 0) child.measuredWidth else lineWidth + horizontalSpacing + child.measuredWidth
                if (needed > maxWidth && lineWidth > 0) {
                    layoutLine()
                    line = mutableListOf(child)
                    lineWidth = child.measuredWidth
                } else {
                    line.add(child)
                    lineWidth = needed
                }
            }
            layoutLine()
        }
    }

    // endregion

    companion object {
        const val MAX_RATING = 5

        /** A partir desta nota o grupo é o positivo (categorias e agradecimento com loja). */
        const val POSITIVE_FROM = 4

        const val COMMENT_MIN = 10
        const val COMMENT_MAX = 300

        private const val STAR_SIZE_DP = 32f
        private const val STAR_SPACING_DP = 12f
        private const val THANKS_STAR_SIZE_DP = 18f
        private const val CHIP_RADIUS_DP = 100f
        private const val DISABLED_ALPHA = 0.45f

        /** Dourado das estrelas do Figma. Cor literal da escala de avaliação, não é token de brand. */
        private val STAR_COLOR = Color.rgb(0xF5, 0xB3, 0x01)

        /** Âmbar do "Pode melhorar", um tom abaixo da estrela para ter contraste no texto. */
        private val MEDIUM_COLOR = Color.rgb(0xE0, 0x9B, 0x00)

        private const val TITLE = "Como está sendo usar o aplicativo?"
        private const val SUBTITLE = "Sua opinião ajuda a gente a melhorar. Leva só 30 segundos."
        private const val SEND = "Enviar avaliação"
        private const val SENDING = "Enviando…"
        private const val SUBMIT_ERROR = "Não foi possível enviar sua avaliação. Tente novamente."
        private const val NOT_NOW = "Agora não"
        private const val CLOSE = "Fechar"
        private const val RATE_IN_STORE = "Avaliar na loja"
        private const val QUESTION_LOW = "O que você não gostou? Assim podemos melhorar o quanto antes."
        private const val QUESTION_MEDIUM = "O que você acredita que podemos melhorar?"
        private const val QUESTION_HIGH = "Ficamos felizes! Fique à vontade para dar um feedback ou falar o que gosta no app."
        private const val COMMENT_PLACEHOLDER = "Ex.: a recarga foi rápida"
        private const val COMMENT_OPTIONAL = "Opcional"
        private const val COMMENT_MIN_FORMAT = "Mínimo de %d caracteres"
        private const val THANKS_NEGATIVE_TITLE = "Recebido. Vamos trabalhar nisso."
        private const val THANKS_NEGATIVE_MESSAGE =
            "Obrigado pelo seu feedback. Vamos trabalhar para melhorar suas próximas experiências com nosso aplicativo."
        private const val THANKS_POSITIVE_TITLE_FORMAT = "Obrigado pelas %d estrelas!"
        private const val THANKS_POSITIVE_MESSAGE =
            "Nos ajude deixando uma avaliação na loja de aplicativo para que mais pessoas encontrem nosso app. Leva menos de um minuto."
        private const val STAR_DESCRIPTION = "%d estrela(s)"
        private const val SELECTED_DESCRIPTION = "Nota selecionada: %d de 5"

        fun present(
            activity: FragmentActivity,
            negativeCategories: List<Category>,
            positiveCategories: List<Category>,
            delegate: Delegate,
        ): DSSRatingBottomSheet {
            val sheet = DSSRatingBottomSheet()
            sheet.delegate = delegate
            sheet.negativeCategories = negativeCategories
            sheet.positiveCategories = positiveCategories
            sheet.show(activity.supportFragmentManager, TAG)
            return sheet
        }

        const val TAG = "DSSRatingBottomSheet"
    }
}
