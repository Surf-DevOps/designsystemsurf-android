# SurfHubDS Android

Port em Android (Kotlin + Views programáticas, sem Compose) do [SurfHubDS iOS](https://gitlab.com/surftelecom/surfmobile/ios/designsystemsurf).

Mesma arquitetura multi-brand do SPM: 1 módulo `core` + 13 módulos `surfhubds-brand-*` (cada um com seus recursos). Distribuído via **JitPack**, igual ao [SurfAPIKitMP](https://github.com/Surf-DevOps/SurfAPIKitMP).

## Brands suportadas

`default`, `matizconecta`, `uber`, `ifood`, `bandsports`, `flachip`, `conecta`, `mega`, `fluxo`, `pafer`, `paguemenos`, `carrefourchip`, `correioscelular`.

## Consumo (apps cliente)

No `settings.gradle.kts` do app:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

No `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.Surf-DevOps.designsystemsurf-android:surfhubds-core:0.1.0")
    // Adicionar apenas o módulo da brand do app:
    implementation("com.github.Surf-DevOps.designsystemsurf-android:surfhubds-brand-uber:0.1.0")
}
```

## Inicialização

Em `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SurfHubDS.initialize(this) // detecta brand pelo applicationId / meta-data
    }
}
```

Ou seleção manual:

```kotlin
SurfHubDS.initialize(this) { Brand.UBER.let(SurfHubDS::defaultThemeFor) }
```

No `AndroidManifest.xml` da brand específica:

```xml
<application ...>
    <meta-data android:name="BRAND_IDENTIFIER" android:value="uber" />
</application>
```

## Estrutura

```
designsystemsurf-android/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties              # GROUP, VERSION_NAME (lido pelo JitPack)
├── jitpack.yml                    # config build pro JitPack
├── gradle/libs.versions.toml      # version catalog
├── surfhubds-core/
│   └── src/main/kotlin/com/surf/surfhubds/
│       ├── SurfHubDS.kt           # entry point
│       ├── brand/                 # Brand, BrandResolver, BrandConfig, BrandInfo
│       ├── theme/                 # Theme, ThemeManager, DefaultTheme, DSSColors
│       │   └── brands/            # 13 *Theme.kt (paletas das brands)
│       ├── tokens/                # DesignTokens, ColorTokens, ComponentStyles
│       ├── font/                  # DSSFont
│       ├── util/                  # Utility, DrawableFactory, dp helpers, etc
│       └── components/            # ~60 DSS* Views portados do iOS
├── surfhubds-brand-default/
├── surfhubds-brand-uber/
├── surfhubds-brand-ifood/
├── surfhubds-brand-bandsports/
├── surfhubds-brand-flachip/
├── surfhubds-brand-conecta/
├── surfhubds-brand-mega/
├── surfhubds-brand-fluxo/
├── surfhubds-brand-pafer/
├── surfhubds-brand-paguemenos/
├── surfhubds-brand-carrefourchip/
├── surfhubds-brand-correioscelular/
├── surfhubds-brand-matizconecta/
└── surfhubds-brand-uber/
```

Cada `surfhubds-brand-*` carrega só `res/drawable/`, `res/font/`, `assets/brand_config.json` daquela brand.

## Mapeamento iOS → Android

| iOS                  | Android                                 |
|----------------------|-----------------------------------------|
| Swift Package        | Gradle multi-module + JitPack           |
| `BrandConfig.plist`  | `assets/brand_config.json`              |
| `Bundle.module`      | `R.drawable`/`R.font` no brand module   |
| `UIColor`            | `@ColorInt Int` + `ColorValue`          |
| `UIFont`             | `Typeface` + `FontSpec(typeface, sizeSp)` |
| `DSSColors.primary`  | `DSSColors.primary()`                   |
| `ThemeManager.shared`| `ThemeManager` (object)                 |
| `ThemeAware`         | `ThemeAware` (interface)                |
| `UIBottomSheet`      | `BottomSheetDialogFragment`             |
| `UICollectionView`   | `RecyclerView`                          |
| `UIViewController`   | `Activity` / `Fragment`                 |
| `SDWebImage`         | `Glide`                                 |
| `Vision/AVCapture`   | `journeyapps/zxing-android-embedded`    |
| `UIImage QRCode`     | `com.google.zxing.qrcode.QRCodeWriter`  |

## Publicação local pra desenvolvimento

```bash
./gradlew publishToMavenLocal
```

Depois, no app, adicione `mavenLocal()` antes de `jitpack.io` nos repositories.

## Adicionar uma brand nova

1. Criar `surfhubds-brand-<nome>/` espelhando uma existente (build.gradle.kts, manifest, assets/brand_config.json, res/values/strings.xml).
2. Adicionar `include(":surfhubds-brand-<nome>")` no `settings.gradle.kts`.
3. Adicionar entry em `Brand` enum (`com.surf.surfhubds.brand.Brand`).
4. Adicionar detecção em `BrandResolver.detectFromPackage`.
5. Adicionar `<Nome>Theme.kt` em `core/.../theme/brands/` herdando `DefaultTheme` e sobrescrevendo `buildColors()`.
6. Adicionar entry em `SurfHubDS.defaultThemeFor` e `BrandInfo.current`.
7. Publicar nova versão (criar git tag — JitPack compila automaticamente).

## Build / CI

JitPack lê `jitpack.yml`:

```yaml
jdk: [openjdk17]
before_install:
  - sed -i.bak "s/^VERSION_NAME=.*/VERSION_NAME=${VERSION#v}/" gradle.properties
install:
  - ./gradlew publishToMavenLocal
```

A cada tag (`vX.Y.Z`), JitPack injeta a versão e publica todos os 14 módulos. Apps puxam pelo `com.github.Surf-DevOps.designsystemsurf-android:<modulo>:<versão>`.
