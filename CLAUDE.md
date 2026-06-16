# Regras do Projeto

- Port do SurfHubDS iOS (Swift/UIKit) para Android (Kotlin + Views programáticas).
- Sem Compose. Sem layouts XML — UI 100% construída em Kotlin.
- 1 módulo `:surfhubds-core` + 13 módulos `:surfhubds-brand-*`. Espelha o SPM iOS.
- Distribuído via JitPack no padrão `com.github.Surf-DevOps.designsystemsurf-android:<module>:<version>` (o repo foi renomeado de `SurfHubDS-Android` → `designsystemsurf-android`; o coordinate do JitPack segue o nome atual do repo).
- Sempre que modificar código e o build passar com sucesso, fazer commit e push para origin na branch atual.
- Para releases: SEMPRE bumpar `VERSION_NAME` em `gradle.properties`, commitar, criar a git tag `vX.Y.Z` (igual ao novo VERSION_NAME) e dar push da branch **e** da tag. O push da tag dispara o workflow `.github/workflows/jitpack-trigger.yml`, que requisita o POM no JitPack e aguarda o primeiro build publicar (HTTP 200) — sem isso a versão fica "parada" no site.
- Depois de pushar a tag, acompanhar o run (`gh run watch`) e só considerar o release concluído quando o workflow terminar verde.

## Convenções de port

- Tokens semânticos: usar `DSSColors.<token>()` (não hardcoded hex).
- Reagir a trocas de tema: implementar `ThemeAware { fun applyTheme(theme: Theme) }` + chamar `setupThemeObserver()` no init.
- DP helpers: `Float.dpToPx(context): Int`.
- Drawables com borda+canto: `DrawableFactory.rounded(...)`.
- Fontes: `DSSFont.regular(context, sizeSp).typeface`.
- Imagens de URL: Glide.
- QR/barcode: ZXing (`com.journeyapps.barcodescanner` + `com.google.zxing.qrcode.QRCodeWriter`).
- Bottom sheets: `BottomSheetDialogFragment`.
- Listas: `RecyclerView` + Adapter interna no componente.
