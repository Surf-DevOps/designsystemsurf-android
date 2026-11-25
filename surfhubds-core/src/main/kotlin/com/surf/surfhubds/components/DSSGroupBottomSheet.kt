package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.R
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.DSSWeight
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dssInitial
import com.surf.surfhubds.util.dssText
import com.surf.surfhubds.util.matchWidthParams
import com.surf.surfhubds.util.weightParams
import com.surf.surfhubds.util.wrapParams

/**
 * Bottom sheet dos fluxos do Grupo de Recarga (Figma: "Sair do grupo?", "Remover integrante
 * do grupo?", "Confirmar troca de plano", "Alterar plano de Ana", ações do integrante,
 * "Trocar cartão"). Tudo é opcional e aparece nesta ordem:
 *
 * 1. ícone em badge ([Icon]), título e subtítulo;
 * 2. [Member]: resumo de uma linha (avatar, nome, número e valor à direita);
 * 3. [PlanCompare]: plano atual → novo plano;
 * 4. [Input]: campo de texto ou telefone (adicionar integrante, renomear), depois as
 *    [Choice]s: opções com seleção única (planos, cartões, dias em grade);
 * 5. caixa cinza com [Spec.note] e [InfoRow]s ("Começa a valer em 25/11"); com opções, as
 *    linhas da opção marcada ([Choice.infoRows]) substituem as da Spec;
 * 6. [Action]s: linhas de ação com ícone (ex.: "Alterar plano", "Remover do grupo");
 * 7. lista com marcadores ([Spec.bulletsTitle] + [Spec.bullets], o "O que acontece:");
 * 8. botão principal e link secundário.
 *
 * O conteúdo vai nos argumentos e a resposta volta por Fragment Result (chave
 * [Spec.requestKey]), não por lambda: assim a ação não se perde se a Activity for recriada
 * com o sheet aberto. Escute com [setResultListener] e leia o evento com [resultEvent] e
 * a opção com [resultChoiceId].
 */
class DSSGroupBottomSheet : BottomSheetDialogFragment() {

    enum class Icon(val drawable: Int, val destructive: Boolean, val badge: DSSIconBadge.Style) {
        REMOVE_PERSON(R.drawable.dss_ic_person_remove, true, DSSIconBadge.Style.ERROR),
        SWAP(R.drawable.dss_ic_swap, true, DSSIconBadge.Style.ERROR),
        CARD(R.drawable.dss_creditcard, false, DSSIconBadge.Style.PRIMARY_SOFT),
        EDIT(R.drawable.dss_ic_edit, false, DSSIconBadge.Style.PRIMARY_SOFT),
        TRASH(R.drawable.dss_ic_trash, true, DSSIconBadge.Style.ERROR),
        INFO(R.drawable.dss_ic_info, false, DSSIconBadge.Style.PRIMARY_SOFT),
        SUCCESS(R.drawable.dss_ic_check, false, DSSIconBadge.Style.SUCCESS),
    }

    data class Member(
        val name: String,
        val phone: String,
        val valueLabel: String? = null,
        val value: String? = null,
    )

    data class PlanCompare(
        val current: DSSPlanCompareView.Plan,
        val new: DSSPlanCompareView.Plan,
    )

    data class Choice(
        val id: String,
        val title: String,
        val subtitle: String? = null,
        val value: String? = null,
        val tag: String? = null,
        /** Informações da caixa cinza quando esta opção está marcada (ex.: novo total do grupo). */
        val infoRows: List<InfoRow> = emptyList(),
    )

    data class InfoRow(val label: String, val value: String, val caption: String? = null)

    /** Campo de texto do sheet (adicionar integrante, renomear). O valor volta no resultado. */
    data class Input(
        val label: String,
        val hint: String = "",
        val value: String = "",
        val type: InputType = InputType.TEXT,
        /** Mensagem de erro já exibida ao abrir (ex.: recusa do backend ao tentar de novo). */
        val error: String? = null,
        val maxLength: Int = 40,
    )

    /** [PHONE]: máscara de celular e o botão só habilita com 11 dígitos. */
    enum class InputType { TEXT, PHONE }

    data class Action(
        val id: String,
        val icon: Icon,
        val title: String,
        val subtitle: String? = null,
    )

    data class Spec(
        val requestKey: String,
        val icon: Icon? = null,
        val title: String? = null,
        val subtitle: String? = null,
        val member: Member? = null,
        val planCompare: PlanCompare? = null,
        val choices: List<Choice> = emptyList(),
        val selectedChoiceId: String? = null,
        val infoRows: List<InfoRow> = emptyList(),
        /** Texto no topo da caixa cinza ("O novo plano vale a partir de 25/11"). */
        val note: String? = null,
        val actions: List<Action> = emptyList(),
        val bulletsTitle: String? = null,
        val bullets: List<String> = emptyList(),
        val primaryTitle: String? = null,
        val secondaryTitle: String? = null,
        /** Com opções, o botão principal só habilita depois que a seleção muda. */
        val requiresChange: Boolean = false,
        /** `false`: não fecha arrastando nem tocando fora (ex.: sucesso de um pagamento). */
        val cancelable: Boolean = true,
        /** > 1: opções curtas em grade (ex.: dias do mês, 7 por linha) em vez de lista. */
        val choiceColumns: Int = 1,
        val input: Input? = null,
    )

    enum class Event { PRIMARY, SECONDARY, ACTION }

    private var selectedChoiceId: String? = null
    private var inputField: DSSLabelTextField? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val spec = specFrom(requireArguments())
        isCancelable = spec.cancelable
        selectedChoiceId = savedInstanceState?.getString(STATE_SELECTED) ?: spec.selectedChoiceId

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24f.dpToPx(ctx), 12f.dpToPx(ctx), 24f.dpToPx(ctx), 28f.dpToPx(ctx))
        }
        root.addView(View(ctx).apply {
            background = DrawableFactory.rounded(ctx, DSSColors.divider(), 2f)
        }, LinearLayout.LayoutParams(150f.dpToPx(ctx), 4f.dpToPx(ctx)).apply { gravity = Gravity.CENTER_HORIZONTAL })

        spec.icon?.let { icon ->
            val badge = DSSIconBadge(ctx).apply {
                configure(ContextCompat.getDrawable(ctx, icon.drawable), icon.badge)
            }
            root.addView(badge, LinearLayout.LayoutParams(46f.dpToPx(ctx), 46f.dpToPx(ctx)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 28f.dpToPx(ctx)
            })
        }
        val centered = spec.icon != null
        spec.title?.let {
            root.addView(dssText(ctx, DSSWeight.SEMIBOLD, 18f, DSSColors.textPrimary()).apply {
                text = it
                gravity = if (centered) Gravity.CENTER else Gravity.START
            }, matchWidthParams().apply { topMargin = (if (centered) 20f else 28f).dpToPx(ctx) })
        }
        spec.subtitle?.let {
            root.addView(dssText(ctx, DSSWeight.REGULAR, 13f, DSSColors.textSecondary()).apply {
                text = it
                gravity = if (centered) Gravity.CENTER else Gravity.START
            }, matchWidthParams().apply { topMargin = 6f.dpToPx(ctx) })
        }
        spec.member?.let { root.addView(memberBox(ctx, it), matchWidthParams().apply { topMargin = 20f.dpToPx(ctx) }) }
        spec.input?.let { input ->
            val field = DSSLabelTextField(ctx).apply {
                configure(
                    label = input.label,
                    placeholder = input.hint,
                    type = if (input.type == InputType.PHONE) DSSLabelTextField.Type.Phone else DSSLabelTextField.Type.Text,
                    showsErrorLabel = true,
                    fontSize = 14f,
                )
                if (input.type == InputType.TEXT) {
                    editText.filters = arrayOf(android.text.InputFilter.LengthFilter(input.maxLength))
                    editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                }
                text = savedInstanceState?.getString(STATE_INPUT) ?: input.value
                if (savedInstanceState == null) setError(input.error)
            }
            inputField = field
            root.addView(field, matchWidthParams().apply { topMargin = 20f.dpToPx(ctx) })
        }
        spec.planCompare?.let {
            root.addView(DSSPlanCompareView(ctx).apply { configure(it.current, it.new) },
                matchWidthParams().apply { topMargin = 24f.dpToPx(ctx) })
        }

        val primary = DSSPrincipalButton(ctx).apply {
            applyPillStyle(heightDp = 46f, textSizeSp = 16f)
            text = spec.primaryTitle
            onTap = { send(spec, Event.PRIMARY) }
        }
        fun updatePrimary() {
            primary.isEnabled = (!spec.requiresChange || selectedChoiceId != spec.selectedChoiceId) && inputValid(spec)
            primary.alpha = if (primary.isEnabled) 1f else 0.4f
        }

        // Caixa cinza: nota + linhas; com opções, as linhas seguem a opção marcada.
        val infoHolder = FrameLayout(ctx)
        fun updateInfo() {
            infoHolder.removeAllViews()
            val rows = spec.choices.firstOrNull { it.id == selectedChoiceId }?.infoRows?.takeIf { it.isNotEmpty() }
                ?: spec.infoRows
            infoHolder.visibility = if (rows.isEmpty() && spec.note == null) View.GONE else View.VISIBLE
            if (infoHolder.visibility == View.VISIBLE) infoHolder.addView(infoBox(ctx, spec.note, rows))
        }

        if (spec.choices.isNotEmpty() && spec.choiceColumns > 1) {
            root.addView(choiceGrid(ctx, spec) { updatePrimary(); updateInfo() }, matchWidthParams().apply { topMargin = 20f.dpToPx(ctx) })
        } else if (spec.choices.isNotEmpty()) {
            val group = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            val rows = mutableListOf<Pair<String, DSSChoiceList.Row>>()
            spec.choices.forEachIndexed { index, choice ->
                val row = DSSChoiceList.Row(
                    ctx,
                    DSSChoiceList.Item(choice.id, choice.title, subtitle = choice.subtitle, value = choice.value, tag = choice.tag),
                )
                rows += choice.id to row
                row.setOnClickListener {
                    selectedChoiceId = choice.id
                    rows.forEach { (id, r) -> r.setChecked(id == selectedChoiceId) }
                    updatePrimary()
                    updateInfo()
                }
                group.addView(row, matchWidthParams().apply { if (index > 0) topMargin = 10f.dpToPx(ctx) })
            }
            rows.forEach { (id, r) -> r.setChecked(id == selectedChoiceId) }
            root.addView(group, matchWidthParams().apply { topMargin = 20f.dpToPx(ctx) })
        }

        updateInfo()
        root.addView(infoHolder, matchWidthParams().apply { topMargin = 20f.dpToPx(ctx) })

        spec.actions.forEachIndexed { index, action ->
            root.addView(actionRow(ctx, action) { send(spec, Event.ACTION, action.id) },
                matchWidthParams().apply { topMargin = (if (index == 0) 20f else 12f).dpToPx(ctx) })
        }

        if (spec.bullets.isNotEmpty()) {
            root.addView(DSSNoticeList(ctx).apply { configure(spec.bulletsTitle, spec.bullets) },
                matchWidthParams().apply { topMargin = 24f.dpToPx(ctx) })
        }

        inputField?.editText?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                inputField?.setError(null)
                updatePrimary()
            }
        })
        if (spec.primaryTitle != null) {
            updatePrimary()
            root.addView(primary, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 46f.dpToPx(ctx)).apply {
                topMargin = 28f.dpToPx(ctx)
            })
        }
        spec.secondaryTitle?.let { title ->
            root.addView(dssText(ctx, DSSWeight.REGULAR, 16f, DSSColors.primary()).apply {
                text = title
                gravity = Gravity.CENTER
                val p = 10f.dpToPx(ctx)
                setPadding(p, p, p, p)
                setOnClickListener { send(spec, Event.SECONDARY) }
            }, wrapParams().apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 8f.dpToPx(ctx)
            })
        }

        return ScrollView(ctx).apply {
            setBackgroundColor(DSSColors.background())
            addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    override fun onStart() {
        super.onStart()
        // Abre inteiro: com o conteúdo maior que meia tela, o botão ficava escondido.
        (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED, selectedChoiceId)
        inputField?.let { outState.putString(STATE_INPUT, it.text?.toString()) }
    }

    /** Valor do campo sem espaços nas pontas; telefone volta só com os dígitos. */
    private fun inputValue(spec: Spec): String? {
        val raw = inputField?.text?.toString() ?: return null
        return if (spec.input?.type == InputType.PHONE) raw.filter(Char::isDigit) else raw.trim()
    }

    private fun inputValid(spec: Spec): Boolean {
        val input = spec.input ?: return true
        val value = inputValue(spec).orEmpty()
        return if (input.type == InputType.PHONE) value.length == 11 else value.isNotEmpty()
    }

    private fun send(spec: Spec, event: Event, actionId: String? = null) {
        parentFragmentManager.setFragmentResult(
            spec.requestKey,
            bundleOf(
                RESULT_EVENT to event.name,
                RESULT_ACTION_ID to actionId,
                RESULT_CHOICE_ID to selectedChoiceId,
                RESULT_INPUT to inputValue(spec),
            ),
        )
        dismiss()
    }

    private fun memberBox(ctx: Context, member: Member): View = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = DrawableFactory.rounded(ctx, DSSColors.backgroundSecondary(), 10f)
        val p = 16f.dpToPx(ctx)
        setPadding(p, 14f.dpToPx(ctx), p, 14f.dpToPx(ctx))
        val size = 36f.dpToPx(ctx)
        addView(DSSAvatarStack.single(ctx, member.name.dssInitial(), 36f), LinearLayout.LayoutParams(size, size))
        addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(dssText(ctx, DSSWeight.SEMIBOLD, 15f, DSSColors.textPrimary(), maxLines = 1).apply { text = member.name })
            if (member.phone.isNotBlank()) {
                addView(dssText(ctx, DSSWeight.REGULAR, 13f, DSSColors.textSecondary(), maxLines = 1).apply { text = member.phone },
                    wrapParams().apply { topMargin = 2f.dpToPx(ctx) })
            }
        }, weightParams().apply { marginStart = 14f.dpToPx(ctx) })
        if (member.value != null) {
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                member.valueLabel?.let { addView(dssText(ctx, DSSWeight.REGULAR, 12f, DSSColors.textSecondary()).apply { text = it }) }
                addView(dssText(ctx, DSSWeight.BOLD, 15f, DSSColors.textPrimary()).apply { text = member.value })
            }, wrapParams().apply { marginStart = 8f.dpToPx(ctx) })
        }
    }

    private fun infoBox(ctx: Context, note: String?, rows: List<InfoRow>): View = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = DrawableFactory.rounded(ctx, DSSColors.backgroundSecondary(), 10f)
        val p = 16f.dpToPx(ctx)
        setPadding(p, 4f.dpToPx(ctx), p, 4f.dpToPx(ctx))
        note?.let {
            addView(dssText(ctx, DSSWeight.REGULAR, 13f, DSSColors.textSecondary()).apply {
                text = it
                val v = 12f.dpToPx(ctx)
                setPadding(0, v, 0, if (rows.isEmpty()) v else 4f.dpToPx(ctx))
            }, matchWidthParams())
        }
        rows.forEachIndexed { index, row ->
            if (index > 0) addView(View(ctx).apply { setBackgroundColor(DSSColors.divider()) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1f.dpToPx(ctx)))
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val v = 12f.dpToPx(ctx)
                setPadding(0, v, 0, v)
                addView(dssText(ctx, DSSWeight.REGULAR, 14f, DSSColors.textSecondary()).apply { text = row.label }, weightParams())
                addView(LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.END
                    addView(dssText(ctx, DSSWeight.BOLD, 14f, DSSColors.textPrimary()).apply { text = row.value })
                    row.caption?.let { addView(dssText(ctx, DSSWeight.REGULAR, 11f, DSSColors.textTertiary()).apply { text = it }) }
                }, wrapParams())
            }, matchWidthParams())
        }
    }

    /** Grade de opções curtas: a marcada fica cheia na cor do botão principal. */
    private fun choiceGrid(ctx: Context, spec: Spec, onChange: () -> Unit): View {
        val grid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val cells = mutableListOf<Pair<String, TextView>>()
        fun paint() = cells.forEach { (id, cell) ->
            val selected = id == selectedChoiceId
            val fill = if (selected) DSSColors.primaryButton() else DSSColors.background()
            cell.background = DrawableFactory.rounded(ctx, fill, 10f, if (selected) fill else DSSColors.divider(), 1f)
            cell.setTextColor(if (selected) DSSColors.contrastOn(fill) else DSSColors.textPrimary())
        }
        spec.choices.chunked(spec.choiceColumns).forEachIndexed { rowIndex, rowChoices ->
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            for (column in 0 until spec.choiceColumns) {
                val choice = rowChoices.getOrNull(column)
                val cell = dssText(ctx, DSSWeight.SEMIBOLD, 15f, DSSColors.textPrimary(), maxLines = 1).apply {
                    gravity = Gravity.CENTER
                    if (choice == null) {
                        visibility = View.INVISIBLE
                    } else {
                        text = choice.title
                        contentDescription = choice.subtitle ?: choice.title
                        setOnClickListener {
                            selectedChoiceId = choice.id
                            paint()
                            onChange()
                        }
                        cells += choice.id to this
                    }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, 42f.dpToPx(ctx), 1f).apply {
                    if (column > 0) marginStart = 6f.dpToPx(ctx)
                })
            }
            grid.addView(row, matchWidthParams().apply { if (rowIndex > 0) topMargin = 6f.dpToPx(ctx) })
        }
        paint()
        return grid
    }

    private fun actionRow(ctx: Context, action: Action, onTap: () -> Unit): View = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = DrawableFactory.rippleOver(
            DrawableFactory.rounded(ctx, DSSColors.background(), 12f, DSSColors.divider(), 1f),
            ColorUtils.setAlphaComponent(DSSColors.textPrimary(), 24),
        )
        val p = 16f.dpToPx(ctx)
        setPadding(p, p, 12f.dpToPx(ctx), p)
        val tone = if (action.icon.destructive) DSSColors.error() else DSSColors.textPrimary()
        val badgeSize = 32f.dpToPx(ctx)
        addView(FrameLayout(ctx).apply {
            background = DrawableFactory.rounded(ctx, ColorUtils.setAlphaComponent(tone, 22), 8f)
            addView(ImageView(ctx).apply {
                setImageDrawable(ContextCompat.getDrawable(ctx, action.icon.drawable)?.mutate())
                setColorFilter(tone)
            }, FrameLayout.LayoutParams(16f.dpToPx(ctx), 16f.dpToPx(ctx), Gravity.CENTER))
        }, LinearLayout.LayoutParams(badgeSize, badgeSize))
        addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(dssText(ctx, DSSWeight.SEMIBOLD, 15f, tone).apply { text = action.title })
            action.subtitle?.let {
                addView(dssText(ctx, DSSWeight.REGULAR, 12f, DSSColors.textSecondary()).apply { text = it },
                    wrapParams().apply { topMargin = 2f.dpToPx(ctx) })
            }
        }, weightParams().apply { marginStart = 14f.dpToPx(ctx) })
        addView(ImageView(ctx).apply {
            setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.dss_ic_chevron_right)?.mutate())
            setColorFilter(DSSColors.textPrimary())
        }, LinearLayout.LayoutParams(22f.dpToPx(ctx), 22f.dpToPx(ctx)))
        setOnClickListener { onTap() }
        contentDescription = listOfNotNull(action.title, action.subtitle).joinToString(". ")
    }

    companion object {
        private const val ARG_SPEC = "dss_group_sheet_spec"
        private const val STATE_SELECTED = "dss_group_sheet_selected"
        private const val RESULT_EVENT = "event"
        private const val RESULT_ACTION_ID = "actionId"
        private const val RESULT_CHOICE_ID = "choiceId"
        private const val RESULT_INPUT = "input"
        private const val STATE_INPUT = "dss_group_sheet_input"

        fun show(fragmentManager: FragmentManager, spec: Spec, tag: String = spec.requestKey): DSSGroupBottomSheet =
            DSSGroupBottomSheet().apply {
                arguments = bundleOf(ARG_SPEC to spec.toBundle())
                show(fragmentManager, tag)
            }

        /**
         * Registra o retorno do sheet. Chame no `onCreate`/`onViewCreated` de quem abre,
         * sempre (não só ao abrir), para receber o resultado também depois de uma recriação.
         */
        fun setResultListener(
            fragmentManager: FragmentManager,
            requestKey: String,
            owner: androidx.lifecycle.LifecycleOwner,
            listener: (event: Event, actionId: String?, choiceId: String?) -> Unit,
        ) {
            fragmentManager.setFragmentResultListener(requestKey, owner) { _, result ->
                listener(resultEvent(result), result.getString(RESULT_ACTION_ID), resultChoiceId(result))
            }
        }

        /** Resultado completo, para sheets com campo de texto ([Spec.input]). */
        data class Result(val event: Event, val actionId: String?, val choiceId: String?, val input: String?)

        fun listenResult(
            fragmentManager: FragmentManager,
            requestKey: String,
            owner: androidx.lifecycle.LifecycleOwner,
            listener: (Result) -> Unit,
        ) {
            fragmentManager.setFragmentResultListener(requestKey, owner) { _, result ->
                listener(Result(resultEvent(result), result.getString(RESULT_ACTION_ID), resultChoiceId(result), result.getString(RESULT_INPUT)))
            }
        }

        fun resultEvent(result: Bundle): Event = Event.valueOf(result.getString(RESULT_EVENT) ?: Event.SECONDARY.name)
        fun resultChoiceId(result: Bundle): String? = result.getString(RESULT_CHOICE_ID)

        // --- Serialização manual da Spec em Bundle (sem parcelize no módulo core). ---

        private fun Spec.toBundle(): Bundle = bundleOf(
            "requestKey" to requestKey,
            "icon" to icon?.name,
            "title" to title,
            "subtitle" to subtitle,
            "member" to member?.let { bundleOf("name" to it.name, "phone" to it.phone, "valueLabel" to it.valueLabel, "value" to it.value) },
            "compare" to planCompare?.let {
                bundleOf("current" to it.current.toBundle(), "new" to it.new.toBundle())
            },
            "choices" to ArrayList(choices.map {
                bundleOf("id" to it.id, "title" to it.title, "subtitle" to it.subtitle, "value" to it.value, "tag" to it.tag, "info" to it.infoRows.toBundles())
            }),
            "selected" to selectedChoiceId,
            "info" to infoRows.toBundles(),
            "note" to note,
            "actions" to ArrayList(actions.map { bundleOf("id" to it.id, "icon" to it.icon.name, "title" to it.title, "subtitle" to it.subtitle) }),
            "bulletsTitle" to bulletsTitle,
            "bullets" to ArrayList(bullets),
            "primary" to primaryTitle,
            "secondary" to secondaryTitle,
            "requiresChange" to requiresChange,
            "cancelable" to cancelable,
            "columns" to choiceColumns,
            "input" to input?.let {
                bundleOf("label" to it.label, "hint" to it.hint, "value" to it.value, "type" to it.type.name, "error" to it.error, "max" to it.maxLength)
            },
        )

        private fun List<InfoRow>.toBundles() =
            ArrayList(map { bundleOf("label" to it.label, "value" to it.value, "caption" to it.caption) })

        private fun Bundle.infoRows(key: String): List<InfoRow> =
            bundles(key).map { InfoRow(it.getString("label").orEmpty(), it.getString("value").orEmpty(), it.getString("caption")) }

        private fun DSSPlanCompareView.Plan.toBundle() =
            bundleOf("label" to label.toString(), "data" to data.toString(), "price" to price.toString())

        private fun Bundle.plan(key: String): DSSPlanCompareView.Plan? = getBundle(key)?.let {
            DSSPlanCompareView.Plan(it.getString("label").orEmpty(), it.getString("data").orEmpty(), it.getString("price").orEmpty())
        }

        @Suppress("DEPRECATION")
        private fun Bundle.bundles(key: String): List<Bundle> =
            getParcelableArrayList<Bundle>(key).orEmpty()

        internal fun specFrom(args: Bundle): Spec {
            val b = requireNotNull(args.getBundle(ARG_SPEC)) { "DSSGroupBottomSheet sem Spec; use show()" }
            return Spec(
                requestKey = b.getString("requestKey").orEmpty(),
                icon = b.getString("icon")?.let { Icon.valueOf(it) },
                title = b.getString("title"),
                subtitle = b.getString("subtitle"),
                member = b.getBundle("member")?.let {
                    Member(it.getString("name").orEmpty(), it.getString("phone").orEmpty(), it.getString("valueLabel"), it.getString("value"))
                },
                planCompare = b.getBundle("compare")?.let { c ->
                    val current = c.plan("current")
                    val new = c.plan("new")
                    if (current != null && new != null) PlanCompare(current, new) else null
                },
                choices = b.bundles("choices").map {
                    Choice(it.getString("id").orEmpty(), it.getString("title").orEmpty(), it.getString("subtitle"), it.getString("value"), it.getString("tag"), it.infoRows("info"))
                },
                selectedChoiceId = b.getString("selected"),
                infoRows = b.infoRows("info"),
                note = b.getString("note"),
                actions = b.bundles("actions").map {
                    Action(it.getString("id").orEmpty(), Icon.valueOf(it.getString("icon").orEmpty()), it.getString("title").orEmpty(), it.getString("subtitle"))
                },
                bulletsTitle = b.getString("bulletsTitle"),
                bullets = b.getStringArrayList("bullets").orEmpty(),
                primaryTitle = b.getString("primary"),
                secondaryTitle = b.getString("secondary"),
                requiresChange = b.getBoolean("requiresChange"),
                cancelable = b.getBoolean("cancelable", true),
                choiceColumns = b.getInt("columns", 1),
                input = b.getBundle("input")?.let {
                    Input(
                        it.getString("label").orEmpty(), it.getString("hint").orEmpty(), it.getString("value").orEmpty(),
                        InputType.valueOf(it.getString("type") ?: InputType.TEXT.name), it.getString("error"), it.getInt("max", 40),
                    )
                },
            )
        }
    }
}
