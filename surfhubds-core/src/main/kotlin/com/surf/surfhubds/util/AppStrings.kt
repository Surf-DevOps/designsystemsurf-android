package com.surf.surfhubds.util

import android.content.Context
import androidx.annotation.StringRes

/**
 * Port do `AppStrings.swift` do iOS. No Android, o sistema de localização nativo
 * via `R.string` já cobre a maior parte — este helper é açúcar para
 * `context.getString(id, *args)` mantendo a mesma fachada.
 */
object AppStrings {

    fun t(context: Context, @StringRes id: Int): String = context.getString(id)

    fun t(context: Context, @StringRes id: Int, vararg args: Any): String =
        context.getString(id, *args)

    /**
     * Resolve uma string da brand pelo nome do resource (ex.: `resume_card_title`),
     * espelhando o `AppStrings.t("resume_card.title")` do iOS — onde os componentes do
     * DSS leem os textos do bundle da brand. No Android os módulos `surfhubds-brand-*`
     * mesclam `res/values/strings.xml` no app host, então o lookup é por
     * `getIdentifier`. Se a brand não definir a chave, usa [fallback].
     */
    fun brand(context: Context, name: String, fallback: String, vararg args: Any): String {
        val resId = context.resources.getIdentifier(name, "string", context.packageName)
        return if (resId != 0) {
            if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
        } else {
            if (args.isEmpty()) fallback else fallback.format(*args)
        }
    }

    /** Lookup por chave (key→default), útil quando vier de remote config. */
    private val builtIn: Map<String, String> = mapOf(
        "home.title" to "Educação Inteligente SP - Bem-vindo",
        "home.subtitle" to "Sua jornada educacional começa aqui",
        "home.cta" to "Entrar",
        "home.welcome_message" to "Bem-vindo ao sistema de Educação Inteligente",
        "desc_terms" to "Para acessar o aplicativo e aproveitar todas as vantagens você precisa estar de acordo com nosso Termos de Uso e Política de Privacidade. Nenhuma informação será divulgada e qualquer dado informado é protegido por um ambiente 100% seguro.",
        "accept_terms" to "Aceite Termos de Uso e Política de Privacidade.",
        "offer_regulations" to "Aceite Regulamento de Ofertas.",
        "accept_and_continue" to "Aceitar e continuar",
        "brand_name" to "Educação Inteligente Maringá",
        "auth.email.placeholder" to "Digite seu email",
        "auth.password.placeholder" to "Digite sua senha",
        "auth.login.button" to "Entrar",
        "auth.forgot.password" to "Esqueci minha senha",
        "auth.create.account" to "Criar conta",
        "auth.email.required" to "Email é obrigatório",
        "auth.password.required" to "Senha é obrigatória",
        "general.loading" to "Carregando...",
        "general.error" to "Erro",
        "general.cancel" to "Cancelar",
        "general.confirm" to "Confirmar",
        "general.save" to "Salvar",
        "terms_privacy" to "Termos de Uso e Políticas de Privacidade",
        "legal.terms" to "Termos de Uso",
        "legal.privacy" to "Política de Privacidade",
    )

    fun get(key: String): String = builtIn[key] ?: key
}
