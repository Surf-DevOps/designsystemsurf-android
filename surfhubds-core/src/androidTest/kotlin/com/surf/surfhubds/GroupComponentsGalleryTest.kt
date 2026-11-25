package com.surf.surfhubds

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.surf.surfhubds.components.DSSAvatarStack
import com.surf.surfhubds.components.DSSGroupBottomSheet
import com.surf.surfhubds.components.DSSGroupCard
import com.surf.surfhubds.components.DSSGroupHeaderCard
import com.surf.surfhubds.components.DSSGroupMemberList
import com.surf.surfhubds.components.DSSGroupMemberRow
import com.surf.surfhubds.components.DSSNoticeList
import com.surf.surfhubds.components.DSSPlanCompareView
import com.surf.surfhubds.components.DSSStatusActionCard
import com.surf.surfhubds.components.DSSStatusLabel
import com.surf.surfhubds.components.DSSToggleCard
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.brands.FlachipTheme
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.dpToPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Galeria dos componentes do Grupo de Recarga: monta cada um com dados do Figma, garante
 * que medem com altura > 0 e salva um PNG por tema em `getExternalFilesDir("gallery")`
 * (puxe com `adb pull`) para conferência visual.
 */
@RunWith(AndroidJUnit4::class)
class GroupComponentsGalleryTest {

    private val instr = InstrumentationRegistry.getInstrumentation()

    @Test
    fun galleryLight() = render(ColorScheme.LIGHT)

    @Test
    fun galleryDark() = render(ColorScheme.DARK)

    @Test
    fun sheetReturnsResultAfterRecreate() {
        ThemeManager.setTheme(FlachipTheme())
        ActivityScenario.launch(DialogHostActivity::class.java).use { sc ->
            sc.onActivity { a ->
                DSSGroupBottomSheet.show(a.supportFragmentManager, removeSpec())
            }
            instr.waitForIdleSync()
            sc.recreate()
            instr.waitForIdleSync()
            var event: DSSGroupBottomSheet.Event? = null
            sc.onActivity { a ->
                DSSGroupBottomSheet.setResultListener(a.supportFragmentManager, "remove", a) { e, _, _ -> event = e }
                val sheet = a.supportFragmentManager.findFragmentByTag("remove") as DSSGroupBottomSheet
                findText(sheet.requireView(), "Remover integrante")!!.performClick()
            }
            instr.waitForIdleSync()
            assertEquals(DSSGroupBottomSheet.Event.PRIMARY, event)
        }
    }

    private fun render(scheme: ColorScheme) {
        ThemeManager.setTheme(FlachipTheme())
        ThemeManager.setColorScheme(scheme)
        val tag = scheme.name.lowercase()
        ActivityScenario.launch(DialogHostActivity::class.java).use { sc ->
            sc.onActivity { a ->
                val column = gallery(a)
                save(a, column, "components_$tag")
            }
            listOf(removeSpec(), planSpec(), compareSpec(), actionsSpec(), addSpec(), daySpec()).forEach { spec ->
                sc.onActivity { a -> DSSGroupBottomSheet.show(a.supportFragmentManager, spec) }
                instr.waitForIdleSync()
                sc.onActivity { a ->
                    val sheet = a.supportFragmentManager.findFragmentByTag(spec.requestKey) as DSSGroupBottomSheet
                    save(a, sheet.requireView(), "sheet_${spec.requestKey}_$tag")
                    sheet.dismissNow()
                }
            }
        }
        ThemeManager.setColorScheme(ColorScheme.LIGHT)
    }

    private fun gallery(a: AppCompatActivity): LinearLayout {
        val ctx = a
        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DSSColors.background())
            val p = 16f.dpToPx(ctx)
            setPadding(p, p, p, p)
        }
        fun add(v: View, top: Float = 20f) =
            column.addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = top.dpToPx(ctx) })

        add(com.surf.surfhubds.components.DSSTabRow(ctx).apply {
            configure(listOf("Administrador", "Integrante"), selectedIndex = 0)
            minimumHeight = 48f.dpToPx(ctx)
        }, 0f)
        add(DSSAvatarStack(ctx).apply { configureNames(listOf("Gustavo", "Ana", "Pedro", "Vó Lúcia", "Lucas"), maxVisible = 4) })
        add(LinearLayout(ctx).apply {
            addView(DSSStatusLabel(ctx).apply { configure("Programada", DSSStatusLabel.Tone.SUCCESS) })
            addView(DSSStatusLabel(ctx).apply { configure("Sem programada", DSSStatusLabel.Tone.NEUTRAL) },
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = 16f.dpToPx(ctx) })
            addView(DSSStatusLabel(ctx).apply { configure("Inativa", DSSStatusLabel.Tone.NEUTRAL, DSSStatusLabel.Style.PILL) },
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = 16f.dpToPx(ctx) })
            addView(DSSStatusLabel(ctx).apply { configure("Vinculado", DSSStatusLabel.Tone.SUCCESS, DSSStatusLabel.Style.PILL) },
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = 16f.dpToPx(ctx) })
        })
        add(DSSGroupCard(ctx).apply {
            configure(DSSGroupCard.Model(
                name = "Grupo 1", memberNames = listOf("Gustavo", "Ana", "Pedro", "Vó Lúcia", "Lucas"),
                membersText = "5 integrantes", statusText = "Programada", statusTone = DSSStatusLabel.Tone.SUCCESS,
                infos = listOf(DSSGroupCard.Info("Próxima recarga", "25/11"), DSSGroupCard.Info("Valor da recarga", "R$ 235,00")),
                secondaryAction = "Ver grupo", primaryAction = "Recarregar",
            ))
        })
        add(DSSGroupCard(ctx).apply {
            configure(DSSGroupCard.Model(
                name = "Família Souza", memberNames = listOf("Gustavo", "Ana", "Pedro"),
                membersText = "3 integrantes", statusText = "Programada", statusTone = DSSStatusLabel.Tone.SUCCESS,
                infos = listOf(DSSGroupCard.Info("Próxima recarga", "25/11"), DSSGroupCard.Info("Valor da recarga", "R$ 120,00")),
                secondaryAction = "Sair do grupo",
            ))
        })
        add(DSSGroupHeaderCard(ctx).apply {
            onEdit = {}
            configure(DSSGroupHeaderCard.Model(
                "Grupo 1", listOf("Gustavo", "Ana", "Pedro", "Vó Lúcia", "Lucas"), "5/100 integrantes",
                "Valor da recarga", "R$ 235,00", "Recarregar grupo",
            ))
        })
        add(HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(ctx).apply {
                addView(DSSStatusActionCard(ctx).apply {
                    configure(DSSStatusActionCard.Model(
                        ContextCompat.getDrawable(ctx, R.drawable.dss_ic_calendar_clock), "Recarga programada",
                        "Renovação automática todo mês", "Inativa", DSSStatusLabel.Tone.NEUTRAL, "Programar recarga",
                    ))
                }, LinearLayout.LayoutParams(290f.dpToPx(ctx), -2))
                addView(DSSStatusActionCard(ctx).apply {
                    configure(DSSStatusActionCard.Model(
                        ContextCompat.getDrawable(ctx, R.drawable.dss_creditcard), "Cartão das recargas",
                        "Visa •••• 4321 · validade 08/29", "Vinculado", DSSStatusLabel.Tone.SUCCESS, "Trocar cartão",
                    ))
                }, LinearLayout.LayoutParams(290f.dpToPx(ctx), -2).apply { marginStart = 12f.dpToPx(ctx) })
            })
        })
        add(DSSGroupMemberList(ctx).apply {
            onMemberTap = {}
            configure(listOf(
                DSSGroupMemberRow.Model("Você", "11 9 1234-5678", "R$30", badge = "ADMIN", avatarName = "Gustavo"),
                DSSGroupMemberRow.Model("Ana Souza", "11 9 2222-1010", "R$30"),
                DSSGroupMemberRow.Model("Pedro Souza", "11 9 3333-2020", "R$30"),
            ))
        })
        add(DSSToggleCard(ctx).apply {
            configure("Mesmo plano para todos", "Desligue para escolher um plano para cada linha.", true)
        })
        add(DSSGroupMemberRow(ctx).apply {
            style = DSSGroupMemberRow.Style.CARD
            onTap = {}
            configure(DSSGroupMemberRow.Model("Irmão", "11 9 0000-0000", "Até 35GB · R$ 40,00", showsAvatar = false))
        })
        add(DSSPlanCompareView(ctx).apply {
            configure(DSSPlanCompareView.Plan("Plano atual", "Até 35GB", "R$ 40,00"), DSSPlanCompareView.Plan("Novo plano", "Até 20GB", "R$ 30,00"))
        })
        add(DSSNoticeList(ctx).apply {
            configure("Importante", listOf(
                "Não é possível adicionar um número mais de uma vez no mesmo grupo;",
                "Todos os números cadastrados em um grupo devem ser do Fla-Chip.",
            ))
        })
        return column
    }

    private fun addSpec() = DSSGroupBottomSheet.Spec(
        requestKey = "add", title = "Adicionar integrante", subtitle = "Só números do Fla-Chip, sem repetir.",
        input = DSSGroupBottomSheet.Input("Número do integrante", "(11) 9 0000-0000", "11977770000",
            DSSGroupBottomSheet.InputType.PHONE, error = "Este número não é Fla-Chip."),
        primaryTitle = "Adicionar", secondaryTitle = "Cancelar",
    )

    private fun daySpec() = DSSGroupBottomSheet.Spec(
        requestKey = "day", title = "Dia da recarga programada", subtitle = "As recargas do grupo são feitas todo mês, no dia escolhido.",
        choices = (1..28).map { DSSGroupBottomSheet.Choice(it.toString(), it.toString()) }, choiceColumns = 7,
        selectedChoiceId = "10", primaryTitle = "Confirmar dia",
    )

    private fun removeSpec() = DSSGroupBottomSheet.Spec(
        requestKey = "remove", icon = DSSGroupBottomSheet.Icon.REMOVE_PERSON, title = "Remover integrante do grupo?",
        member = DSSGroupBottomSheet.Member("Pedro Souza", "11 9 3333-2020", "Plano", "R$ 30,00"),
        bulletsTitle = "O que acontece:",
        bullets = listOf(
            "Pedro deixa de receber as recargas do grupo a partir da próxima programada (25/11).",
            "A recarga atual dele continua valendo até o vencimento.",
        ),
        primaryTitle = "Remover integrante", secondaryTitle = "Cancelar",
    )

    private fun planSpec() = DSSGroupBottomSheet.Spec(
        requestKey = "plan", title = "Alterar plano de Ana", subtitle = "11 9 2222-1010",
        choices = listOf(
            DSSGroupBottomSheet.Choice("20", "Até 20GB", value = "R$ 30,00"),
            DSSGroupBottomSheet.Choice("35", "Até 35GB", value = "R$ 40,00", tag = "Plano atual"),
            DSSGroupBottomSheet.Choice("45", "Até 45GB", value = "R$ 50,00"),
        ),
        selectedChoiceId = "35", requiresChange = true,
        infoRows = listOf(DSSGroupBottomSheet.InfoRow("Valor da recarga do grupo", "R$ 235,00")),
        primaryTitle = "Alterar oferta",
    )

    private fun compareSpec() = DSSGroupBottomSheet.Spec(
        requestKey = "compare", icon = DSSGroupBottomSheet.Icon.SWAP, title = "Confirmar troca de plano",
        subtitle = "Confira a alteração para a linha de Ana (11 9 2222-1010).",
        planCompare = DSSGroupBottomSheet.PlanCompare(
            DSSPlanCompareView.Plan("Plano atual", "Até 35GB", "R$ 40,00"),
            DSSPlanCompareView.Plan("Novo plano", "Até 20GB", "R$ 30,00"),
        ),
        infoRows = listOf(
            DSSGroupBottomSheet.InfoRow("Começa a valer em", "25/11"),
            DSSGroupBottomSheet.InfoRow("Valor da recarga do grupo", "R$ 225,00", "antes R$ 235,00"),
        ),
        primaryTitle = "Confirmar troca", secondaryTitle = "Voltar",
    )

    private fun actionsSpec() = DSSGroupBottomSheet.Spec(
        requestKey = "actions",
        member = DSSGroupBottomSheet.Member("Ana Souza", "11 9 2222-1010"),
        infoRows = listOf(DSSGroupBottomSheet.InfoRow("Plano atual", "Até 30GB · R$ 45,00")),
        actions = listOf(
            DSSGroupBottomSheet.Action("change", DSSGroupBottomSheet.Icon.EDIT, "Alterar plano", "Escolha outro plano para esta linha"),
            DSSGroupBottomSheet.Action("remove", DSSGroupBottomSheet.Icon.TRASH, "Remover do grupo", "Não recebe mais recargas do grupo"),
        ),
    )

    private fun save(a: AppCompatActivity, view: View, name: String) {
        val width = 360f.dpToPx(a)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        assertTrue("$name sem altura", view.measuredHeight > 0)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bmp = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply { drawColor(DSSColors.background()) }.also { view.draw(it) }
        val dir = File(a.getExternalFilesDir("gallery"), "").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun findText(v: View, text: String): View? {
        if (v is android.widget.TextView && v.text.toString() == text) return v
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) findText(v.getChildAt(i), text)?.let { return it }
        return null
    }
}
