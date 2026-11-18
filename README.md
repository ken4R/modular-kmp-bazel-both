# modular-kmp-bazel

Projet mobile modulaire, iOS et Android : la logique est écrite une seule fois en Kotlin Multiplatform, l'interface reste native sur chaque plateforme (SwiftUI et Jetpack Compose).

Ce dépôt porte le prototype de la solution A, dite hybride : Gradle construit le code partagé et l'application Android, Bazel construit l'application iOS.

## Ce que le prototype contient

Une feature de référence, `login`, traverse toutes les couches :

| Couche | Emplacement | Contenu |
|---|---|---|
| Logique partagée | `shared/feature/login` | `LoginViewModel`, son état, ses événements, ses tests |
| Brique commune | `shared/core/session` | Session de l'utilisateur, derrière une interface |
| Pont vers iOS | `shared/umbrella` | Framework `Shared`, traduit pour Swift par SKIE |
| Android | `android/feature/login`, `android/app` | Écran Compose, test d'écran, application |
| iOS | `ios/Features/Login`, `ios/App` | Vue SwiftUI, tests, application |

Chaque module iOS est construit par Bazel et reste un paquet Swift : son `Package.swift` est généré depuis son `BUILD.bazel`.

## Démarrer

Outils : JDK 21, le SDK Android, et sur macOS Xcode 26.4 et [Bazelisk](https://github.com/bazelbuild/bazelisk).

Android et code partagé, sur Linux ou macOS :

```sh
./gradlew check                       # tests, lint, règles de dépendance
./gradlew :android:app:assembleDebug  # l'application Android
```

iOS, sur macOS :

```sh
tools/shared/sync          # construit Shared.xcframework depuis le code Kotlin
bazel test //ios/...       # construit l'application iOS et lance ses tests
bazel run //ios:xcodeproj  # génère ios/Modular.xcodeproj
```

Dans l'application, n'importe quelle adresse e-mail se connecte avec le mot de passe `kotlin`.

## Organisation

```
shared/        logique commune, Kotlin Multiplatform (Gradle)
android/       application et écrans Compose (Gradle)
ios/           application et vues SwiftUI, un paquet Swift par module (Bazel)
build-logic/   plugins de convention Gradle, règles de dépendance entre modules
tools/         scripts du dépôt
```

Les règles du dépôt, les types de modules et la marche à suivre pour ajouter une feature sont dans [AGENTS.md](AGENTS.md). Ce fichier sert aux développeurs comme aux agents de code. Chaque zone a le sien : [shared](shared/AGENTS.md), [android](android/AGENTS.md), [ios](ios/AGENTS.md).

## Intégration continue

La CI GitHub Actions lance les contrôles Gradle sur Linux, puis sur macOS le framework partagé, le build et les tests iOS avec Bazel, et le build d'un module comme paquet Swift. Voir [.github/workflows/ci.yml](.github/workflows/ci.yml).

## Valeurs provisoires

L'identifiant d'application `com.example.modular`, le SDK Android minimum 26 et iOS 17 sont des valeurs de prototype, à remplacer quand elles seront décidées.
