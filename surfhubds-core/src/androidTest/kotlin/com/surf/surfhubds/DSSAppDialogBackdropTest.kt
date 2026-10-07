package com.surf.surfhubds

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.surf.surfhubds.components.DSSAppDialog
import com.surf.surfhubds.components.DSSPrincipalButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

class DialogHostActivity : AppCompatActivity()

@RunWith(AndroidJUnit4::class)
class DSSAppDialogBackdropTest {

    private val instr = InstrumentationRegistry.getInstrumentation()

    private fun backdrops(a: AppCompatActivity): Int {
        val root = a.window.decorView as ViewGroup
        return (0 until root.childCount).count { root.getChildAt(it) is ImageView }
    }

    private fun findButton(v: View): DSSPrincipalButton? {
        if (v is DSSPrincipalButton) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) findButton(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun dialogOf(a: AppCompatActivity) =
        a.supportFragmentManager.findFragmentByTag("DSSAppDialog") as DSSAppDialog?

    @Test
    fun okAfterBackgroundLeavesNoBackdrop() {
        ActivityScenario.launch(DialogHostActivity::class.java).use { sc ->
            sc.onActivity { DSSAppDialog.alert(it, "Ops", "Erro") }
            instr.waitForIdleSync()
            sc.onActivity { assertEquals(1, backdrops(it)) }

            // App vai para segundo plano e volta com o alerta aberto.
            sc.moveToState(Lifecycle.State.CREATED)
            sc.moveToState(Lifecycle.State.RESUMED)
            instr.waitForIdleSync()

            sc.onActivity { a -> findButton(dialogOf(a)!!.requireView())!!.performClick() }
            instr.waitForIdleSync()
            sc.onActivity { a ->
                assertEquals("backdrop órfão após OK", 0, backdrops(a))
                assertTrue(dialogOf(a) == null)
            }
        }
    }

    @Test
    fun recreatedDialogClosesItself() {
        ActivityScenario.launch(DialogHostActivity::class.java).use { sc ->
            sc.onActivity { DSSAppDialog.alert(it, "Ops", "Erro") }
            instr.waitForIdleSync()

            sc.recreate() // rotação / troca de tema
            instr.waitForIdleSync()
            sc.onActivity { a ->
                assertEquals("blur preso após recriar", 0, backdrops(a))
                assertTrue("dialog vazio continua aberto", dialogOf(a) == null)
            }
        }
    }
}
