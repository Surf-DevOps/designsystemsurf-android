package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dpToPxFloat

/**
 * Port do `DSSSelectorType` do iOS — abas com indicador embaixo
 * (Internet / Ligações / SMS), usado nas telas de consumo.
 */
class DSSSelectorType @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    enum class SelectorType(val title: String) {
        INTERNET("Internet"),
        LIGACOES("Ligações"),
        SMS("SMS");
    }

    interface Delegate {
        fun didSelectType(type: SelectorType)
    }

    var delegate: Delegate? = null
    var onSelectionChanged: ((SelectorType) -> Unit)? = null

    val selectedType: SelectorType?
        get() = SelectorType.values().getOrNull(selectedIndex)

    private val buttons = mutableListOf<AppCompatButton>()
    private val indicatorViews = mutableListOf<View>()
    private val backgroundLines = mutableListOf<View>()

    private val selectedColor get() = DSSColors.primary()
    // iOS: UIColor.darkGray == white 1/3 == rgb(85,85,85). (Android Color.DKGRAY é 68,68,68.)
    private val unselectedColor: Int = Color.rgb(85, 85, 85)
    private val lineColor: Int = Color.argb(255, 230, 230, 230) // iOS: UIColor(white: 0.9, alpha: 1)
    private val indicatorHeightDp: Float = 5f
    private val indicatorWidthDp: Float = 30f

    private var selectedIndex: Int = 0

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    init {
        setupView()
        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        setupThemeObserver()
    }

    private fun setupView() {
        container.removeAllViews()
        buttons.clear()
        indicatorViews.clear()
        backgroundLines.clear()

        SelectorType.values().forEachIndexed { index, type ->
            val itemContainer = FrameLayout(context)

            val button = AppCompatButton(context).apply {
                text = when (type) {
                    SelectorType.INTERNET -> AppStrings.brand(context, "data_view_internet", "Internet")
                    SelectorType.LIGACOES -> AppStrings.brand(context, "my_plan_feature_calls_title", "Ligações")
                    SelectorType.SMS -> type.title
                }
                isAllCaps = false
                textSize = 18f
                typeface = DSSFont.light(context, 18f).typeface
                background = null
                // iOS: botão sem padding horizontal; só reservamos o espaço inferior
                // ocupado pela linha/indicador (que no iOS ficam abaixo de button.bottom).
                setPadding(
                    0,
                    0,
                    0,
                    indicatorHeightDp.dpToPx(context) + 4f.dpToPx(context),
                )
                setTextColor(if (index == selectedIndex) selectedColor else unselectedColor)
                tag = index
                setOnClickListener { handleTap(index) }
            }

            val backgroundLine = View(context).apply {
                setBackgroundColor(lineColor)
            }

            val indicator = View(context).apply {
                background = indicatorDrawable(if (index == selectedIndex) selectedColor else Color.TRANSPARENT)
            }

            // Stacking: button | background line + indicator overlap at bottom.
            // BOTTOM (não TOP): o texto encosta na linha/indicador (o padding inferior
            // do botão reserva o espaço do indicador), igual ao iOS. Com TOP o texto
            // flutuava no topo do container, muito alto em relação à linha.
            val buttonLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            }
            itemContainer.addView(button, buttonLp)

            val lineLp = LayoutParams(LayoutParams.MATCH_PARENT, 2f.dpToPx(context)).apply {
                gravity = Gravity.BOTTOM
                leftMargin = 20f.dpToPx(context)
                rightMargin = 20f.dpToPx(context)
            }
            itemContainer.addView(backgroundLine, lineLp)

            val indicatorLp = LayoutParams(
                indicatorWidthDp.dpToPx(context),
                indicatorHeightDp.dpToPx(context),
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            itemContainer.addView(indicator, indicatorLp)

            buttons.add(button)
            backgroundLines.add(backgroundLine)
            indicatorViews.add(indicator)

            val itemLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            container.addView(itemContainer, itemLp)
        }
    }

    private fun handleTap(index: Int) {
        selectedIndex = index
        updateUI()
        val type = SelectorType.values()[index]
        delegate?.didSelectType(type)
        onSelectionChanged?.invoke(type)
    }

    private fun updateUI() {
        for ((index, button) in buttons.withIndex()) {
            val isSelected = index == selectedIndex
            button.setTextColor(if (isSelected) selectedColor else unselectedColor)
            indicatorViews[index].background =
                indicatorDrawable(if (isSelected) selectedColor else Color.TRANSPARENT)
        }
    }

    /**
     * iOS: `cornerRadius = 4` com `maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]`
     * (apenas cantos SUPERIORES). `DrawableFactory.rounded` arredonda os 4 cantos, então aqui
     * montamos um [GradientDrawable] arredondando só o topo (top-left + top-right).
     */
    private fun indicatorDrawable(backgroundColor: Int): GradientDrawable {
        val r = 4f.dpToPxFloat(context)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(backgroundColor)
            // ordem: TL, TL, TR, TR, BR, BR, BL, BL (x/y por canto)
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
    }

    fun setSelected(type: SelectorType) {
        val index = SelectorType.values().indexOf(type).coerceAtLeast(0)
        handleTap(index)
    }

    override fun applyTheme(theme: Theme) {
        updateUI()
    }
}
