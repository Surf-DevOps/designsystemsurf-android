package com.surf.surfhubds.theme

import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.tokens.DesignTokens
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Equivalente ao `ThemeManager` do iOS. Singleton, observer-based.
 *
 * Inicialização típica em `Application.onCreate()`:
 * ```
 * ThemeManager.setTheme(UberTheme())
 * ```
 *
 * Componentes observam via [observe] ou implementando [ThemeAware].
 */
object ThemeManager {

    @Volatile
    private var currentTheme: Theme = NoopTheme

    @Volatile
    private var currentScheme: ColorScheme = ColorScheme.LIGHT

    private val observers = ConcurrentHashMap<UUID, (Theme) -> Unit>()

    val theme: Theme get() = currentTheme
    val tokens: DesignTokens get() = currentTheme.tokens
    val colorScheme: ColorScheme get() = currentScheme

    fun setTheme(theme: Theme) {
        currentTheme = theme
        notifyObservers()
    }

    fun setColorScheme(scheme: ColorScheme) {
        currentScheme = scheme
        notifyObservers()
    }

    /**
     * Registra callback. Retorna o id pra remover depois.
     * Em geral, prefira [bind] (Lifecycle-aware) ou [ThemeAware.setupThemeObserver].
     */
    fun observe(callback: (Theme) -> Unit): UUID {
        val id = UUID.randomUUID()
        observers[id] = callback
        callback(currentTheme)
        return id
    }

    fun removeObserver(id: UUID) {
        observers.remove(id)
    }

    /**
     * Lifecycle-aware: remove o observer automaticamente em `onDestroy`.
     */
    fun bind(lifecycleOwner: LifecycleOwner, callback: (Theme) -> Unit) {
        val id = observe(callback)
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { removeObserver(id) }
        })
    }

    /**
     * Detach automático quando a View é removida da janela.
     */
    fun bind(view: View, callback: (Theme) -> Unit) {
        val id = observe(callback)
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                removeObserver(id)
                v.removeOnAttachStateChangeListener(this)
            }
        })
    }

    private fun notifyObservers() {
        observers.values.forEach { it(currentTheme) }
    }
}

/**
 * Implemente em Views/Fragments/Activities que precisam reagir a trocas de tema.
 *
 * Use [setupThemeObserver] em `onAttachedToWindow` / `onViewCreated` etc.
 */
interface ThemeAware {
    fun applyTheme(theme: Theme)
}

fun View.setupThemeObserver() {
    if (this is ThemeAware) {
        ThemeManager.bind(this) { applyTheme(it) }
    }
}
