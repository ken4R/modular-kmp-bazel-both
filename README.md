# modular-kmp-bazel-both

Projet mobile modulaire, iOS et Android : la logique est écrite une seule fois en Kotlin Multiplatform, l'interface reste native sur chaque plateforme (SwiftUI et Jetpack Compose).

Ce dépôt porte le prototype de la solution B : Gradle construit le code partagé et le publie en binaires, un AAR par module pour Android et un XCFramework pour iOS. Bazel construit les deux applications et leurs modules. Il reprend tel quel le code du [prototype A](https://github.com/ken4R/modular-kmp-bazel), où Gradle construit aussi l'application Android : seuls les fichiers de build changent.

## Ce que le prototype contient

Une feature de référence, `login`, traverse toutes les couches :

| Couche | Emplacement | Contenu |
|---|---|---|
| Logique partagée | `shared/feature/login` | `LoginViewModel`, son état, ses événements, ses tests |
| Brique commune | `shared/core/session` | Session de l'utilisateur, derrière une interface |
| Pont vers Android | `android/shared` | Un AAR par module partagé, publié par Gradle, importé par Bazel |
| Pont vers iOS | `shared/umbrella` | Framework `Shared`, traduit pour Swift par SKIE |
| Android | `android/feature/login`, `android/app` | Écran Compose, tests d'écran, application, construits par Bazel |
| iOS | `ios/Features/Login`, `ios/App` | Vue SwiftUI, tests, application, construits par Bazel |

Gradle et Bazel lisent le même catalogue de versions, `gradle/libs.versions.toml`. Les dépendances Maven de Bazel y sont prises et verrouillées dans `maven_install.json`.

## Démarrer

Outils : JDK 21, [Bazelisk](https://github.com/bazelbuild/bazelisk), le SDK Android avec la plateforme 37, et sur macOS Xcode 26.4.

Code partagé et Android, sur Linux ou macOS :

```sh
./gradlew check                 # tests du code partagé, règles de dépendance
tools/shared/sync --android     # publie les AAR du code partagé pour Bazel
bazel build //android/app       # l'application : bazel-bin/android/app/app.apk
bazel test //android/...        # tests d'écran Robolectric
```

iOS, sur macOS :

```sh
tools/shared/sync --ios    # construit Shared.xcframework depuis le code Kotlin
bazel test //ios/...       # construit l'application iOS et lance ses tests
bazel run //ios:xcodeproj  # génère ios/Modular.xcodeproj
```

Sur macOS, `tools/shared/sync` sans option prépare les deux applications.

Dans l'application, n'importe quelle adresse e-mail se connecte avec le mot de passe `kotlin`.

## Organisation

```
shared/        logique commune, Kotlin Multiplatform (Gradle, publiée en AAR et en XCFramework)
android/       application et écrans Compose, AAR du code partagé (Bazel)
ios/           application et vues SwiftUI, un paquet Swift par module (Bazel)
build-logic/   plugins de convention Gradle, règles de dépendance du code partagé
gradle/        catalogue de versions, lu par Gradle et par Bazel
tools/         scripts du dépôt
```

Les règles du dépôt, les types de modules et la marche à suivre pour ajouter une feature sont dans [AGENTS.md](AGENTS.md). Ce fichier sert aux développeurs comme aux agents de code. Chaque zone a le sien : [shared](shared/AGENTS.md), [android](android/AGENTS.md), [ios](ios/AGENTS.md).

## Intégration continue

La CI GitHub Actions lance sur Linux les contrôles Gradle du code partagé, publie ses AAR, puis construit et teste l'application Android avec Bazel ; l'APK de debug est gardé comme artefact. Sur macOS, elle construit le framework partagé, puis l'application iOS et ses tests avec Bazel, et un module comme paquet Swift. Voir [.github/workflows/ci.yml](.github/workflows/ci.yml).

## Valeurs provisoires

L'identifiant d'application `com.example.modular`, le SDK Android minimum 26 et iOS 17 sont des valeurs de prototype, à remplacer quand elles seront décidées.
