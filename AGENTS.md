# modular-kmp-bazel

Application mobile iOS et Android. La logique est écrite une seule fois en Kotlin Multiplatform. L'interface est native : Jetpack Compose sur Android, SwiftUI sur iOS.

Ce dépôt est le prototype de la solution A : Gradle construit le code partagé et Android, Bazel construit iOS.

## Carte du dépôt

| Zone | Contenu | Construit par | À lire avant d'y toucher |
|---|---|---|---|
| `shared/` | Logique commune, Kotlin Multiplatform | Gradle | `shared/AGENTS.md` |
| `android/` | Application et écrans Compose | Gradle | `android/AGENTS.md` |
| `ios/` | Application et vues SwiftUI, un paquet Swift par module | Bazel | `ios/AGENTS.md` |
| `build-logic/` | Plugins de convention Gradle et règles de dépendance | Gradle | ce fichier |
| `tools/` | Scripts du dépôt | | |

Les deux mondes se rejoignent en un seul fichier, `Shared.xcframework`. Gradle le produit depuis `shared/umbrella`, `tools/shared/sync` le dépose dans `ios/SharedKit/Frameworks/`, et Bazel comme les paquets Swift le lisent à cet endroit.

## Commandes

Sur Linux ou macOS :

- `./gradlew check` : tests du code partagé sur JVM, tests d'écran Compose, lint et règles de dépendance.
- `./gradlew :shared:feature:login:check` : les mêmes contrôles pour un seul module.
- `./gradlew :android:app:assembleDebug` : l'application Android.
- `./gradlew -p build-logic test` : les tests des règles de dépendance.

Sur macOS seulement :

- `./gradlew iosSimulatorArm64Test` : tests du code partagé compilé pour iOS.
- `tools/shared/sync` : construit `Shared.xcframework`. À relancer après tout changement sous `shared/`.
- `bazel test //ios/...` : construit l'application iOS, lance ses tests et vérifie les `Package.swift`.
- `bazel run //ios:xcodeproj` : génère `ios/Modular.xcodeproj`.
- `bazel run //ios/Features/Login:package_swift` : régénère le `Package.swift` d'un module.

Outils attendus : JDK 21 et le SDK Android (`ANDROID_HOME`) partout, Xcode 26.4 et Bazelisk sur macOS. Bazelisk lit la version de Bazel dans `.bazelversion`.

## Types de modules

Un module applique un seul plugin de convention, ou la macro `ios_module`. Le plugin configure tout : cibles, SDK, tests, dépendances communes.

| Dossier | Déclaration | Rôle |
|---|---|---|
| `shared/core/<nom>` | `id("kmp.core")` | Brique commune : session, réseau, modèle |
| `shared/feature/<nom>` | `id("kmp.feature")` | Logique d'une feature : ViewModel, état, événements |
| `shared/umbrella` | `id("kmp.umbrella")` | Produit le framework iOS `Shared` et assemble les dépendances |
| `android/core/<nom>` | `id("android.core")` | Composants Compose communs |
| `android/feature/<nom>` | `id("android.feature")` | Écrans Compose d'une feature |
| `android/app` | `id("android.app")` | Application Android, navigation |
| `ios/Core/<Nom>`, `ios/Features/<Nom>`, `ios/SharedKit` | `ios_module(...)` | Module Swift, aussi paquet Swift |
| `ios/App` | `ios_application(...)` | Application iOS, navigation |

## Règles

1. Aucune logique métier dans `android/` ni dans `ios/`. Un écran affiche un état et transmet des actions.
2. Une feature ne dépend jamais d'une autre feature. Ce qui est commun descend dans un module `core`.
3. Les dépendances entre modules suivent le tableau ci-dessous. Le build les refuse sinon : la tâche Gradle `checkModuleRules` pour `shared/` et `android/`, la visibilité Bazel pour `ios/`.
4. Les versions vivent dans `gradle/libs.versions.toml` et `MODULE.bazel`, jamais dans un module.
5. Les plugins Gradle tiers ne sont appliqués que par `build-logic/`.
6. Ne jamais modifier à la main : les `Package.swift` des modules iOS, `ios/SharedKit/Frameworks/`, les projets Xcode.
7. Le code et ses commentaires sont en anglais. La documentation et les textes affichés à l'utilisateur sont en français.

| Module | Peut dépendre de |
|---|---|
| `shared/core/*` | `shared/core/*` |
| `shared/feature/<nom>` | `shared/core/*` |
| `shared/umbrella` | `shared/core/*`, `shared/feature/*` |
| `android/core/*` | `android/core/*`, `shared/core/*` |
| `android/feature/<nom>` | `shared/feature/<nom>`, `shared/core/*`, `android/core/*` |
| `android/app` | tout |
| `ios/Core/*`, `ios/SharedKit` | `ios/Core/*`, `ios/SharedKit` |
| `ios/Features/<Nom>` | `ios/Core/*`, `ios/SharedKit` |
| `ios/App` | tout |

## Ajouter une feature

La feature `login` sert de modèle : copier sa structure.

1. Logique : créer `shared/feature/<nom>/` avec `id("kmp.feature")`, l'ajouter à `settings.gradle.kts`, écrire le ViewModel et ses tests dans `commonTest`.
2. Pont vers Swift : dans `shared/umbrella/build.gradle.kts`, ajouter le module à `exportToSwift(...)`. Dans `SharedGraph`, ajouter la fonction qui crée le ViewModel.
3. Android : créer `android/feature/<nom>/` avec `id("android.feature")`, l'ajouter à `settings.gradle.kts` et aux dépendances de `android/app`, brancher l'écran dans `AppRoot`.
4. iOS : créer `ios/Features/<Nom>/BUILD.bazel` avec `ios_module`, lancer `bazel run //ios/Features/<Nom>:package_swift`, ajouter le module aux dépendances de `ios/App`, ses tests à `ios/BUILD.bazel`, et brancher la vue dans `RootView`.

## Terminé veut dire

- Les contrôles de la zone touchée passent : `./gradlew check` pour `shared/` et `android/`, `tools/shared/sync` puis `bazel test //ios/...` pour `ios/` et pour tout changement de `shared/` visible depuis Swift.
- Les fichiers générés sont à jour.
- Aucune règle de dépendance n'est enfreinte.
- Le comportement ajouté a un test, dans la couche où il vit.

## Ce qu'un agent peut vérifier

| Contrôle | Linux | macOS |
|---|---|---|
| Tests JVM du code partagé, règles de dépendance | Oui | Oui |
| Build, lint et tests Android | Oui, avec le SDK Android | Oui |
| Framework `Shared` et tests Kotlin/Native | Non | Oui |
| Build et tests iOS avec Bazel, paquets Swift | Non | Oui |

Un agent sur Linux ne peut pas valider un changement iOS. Il s'appuie sur la CI macOS de la pull request et le dit dans sa description : ce qui a été vérifié, et ce qui ne l'a pas été.
