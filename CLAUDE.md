# Regras do Projeto

- Port do SurfHubDS iOS (Swift/UIKit) para Android (Kotlin + Views programáticas).
- Sem Compose. Sem layouts XML — UI 100% construída em Kotlin.
- 1 módulo `:surfhubds-core` + 13 módulos `:surfhubds-brand-*`. Espelha o SPM iOS.
- Distribuído via JitPack no padrão `com.github.Surf-DevOps.SurfHubDS-Android:<module>:<version>`.
- Para releases, criar git tag `vX.Y.Z` — JitPack compila e publica.
- Sempre que modificar código e o build passar com sucesso, fazer commit e push para origin na branch atual.

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
