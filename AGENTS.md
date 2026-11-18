# modular-kmp-bazel-both

Application mobile iOS et Android. La logique est écrite une seule fois en Kotlin Multiplatform. L'interface est native : Jetpack Compose sur Android, SwiftUI sur iOS.

Ce dépôt est le prototype de la solution B : Gradle construit le code partagé et le publie en binaires, Bazel construit les deux applications.

## Carte du dépôt

| Zone | Contenu | Construit par | À lire avant d'y toucher |
|---|---|---|---|
| `shared/` | Logique commune, Kotlin Multiplatform | Gradle | `shared/AGENTS.md` |
| `android/` | Application et écrans Compose, AAR du code partagé | Bazel | `android/AGENTS.md` |
| `ios/` | Application et vues SwiftUI, un paquet Swift par module | Bazel | `ios/AGENTS.md` |
| `build-logic/` | Plugins de convention Gradle et règles de dépendance du code partagé | Gradle | ce fichier |
| `gradle/libs.versions.toml` | Toutes les versions, lues par Gradle et par Bazel | | ce fichier |
| `tools/` | Scripts du dépôt | | |

Les deux mondes se rejoignent en deux points, tous deux remplis par `tools/shared/sync` :

- Android : Gradle publie un AAR par module de `shared/` dans le dépôt Maven local `android/shared/maven/`. Bazel les importe dans `//android/shared`.
- iOS : Gradle produit `Shared.xcframework` depuis `shared/umbrella` et le dépose dans `ios/SharedKit/Frameworks/`. Bazel comme les paquets Swift le lisent à cet endroit.

Ni l'un ni l'autre n'est versionné. Sans eux, Bazel s'arrête et rappelle la commande.

## Commandes

Sur Linux ou macOS :

- `./gradlew check` : tests du code partagé sur JVM et règles de dépendance du code partagé.
- `./gradlew :shared:feature:login:check` : les mêmes contrôles pour un seul module.
- `./gradlew -p build-logic test` : les tests des règles de dépendance.
- `tools/shared/sync --android` : publie les AAR du code partagé. À relancer après tout changement sous `shared/`.
- `bazel build //android/app` : l'application Android, dans `bazel-bin/android/app/app.apk`.
- `bazel test //android/...` : tests d'écran Robolectric, et build de toutes les cibles Android.
- `REPIN=1 bazel run @maven//:pin` : réécrit `maven_install.json` après un changement de `[libraries]` dans le catalogue.

Sur macOS seulement :

- `./gradlew iosSimulatorArm64Test` : tests du code partagé compilé pour iOS.
- `tools/shared/sync` : publie les AAR et construit `Shared.xcframework`. `--ios` pour le framework seul.
- `bazel test //ios/...` : construit l'application iOS, lance ses tests et vérifie les `Package.swift`.
- `bazel run //ios:xcodeproj` : génère `ios/Modular.xcodeproj`.
- `bazel run //ios/Features/Login:package_swift` : régénère le `Package.swift` d'un module.

Sur Linux, lancer `bazel test //android/...` et non `//...` : les tests iOS exigent Xcode.

Outils attendus : JDK 21, Bazelisk et le SDK Android partout, Xcode 26.4 sur macOS. Bazelisk lit la version de Bazel dans `.bazelversion`. Le SDK Android est celui de `ANDROID_HOME`, avec une plateforme 37 ou plus récente et des build-tools 35 ou plus récents : Bazel compile contre la plateforme la plus récente qu'il y trouve.

## Types de modules

Un module Gradle applique un seul plugin de convention. Un module Bazel est déclaré par une seule macro. Le plugin ou la macro configure tout : cibles, SDK, tests, dépendances communes.

| Dossier | Déclaration | Rôle |
|---|---|---|
| `shared/core/<nom>` | `id("kmp.core")` | Brique commune : session, réseau, modèle |
| `shared/feature/<nom>` | `id("kmp.feature")` | Logique d'une feature : ViewModel, état, événements |
| `shared/umbrella` | `id("kmp.umbrella")` | Produit le framework iOS `Shared` et assemble les dépendances |
| `android/shared` | `shared_aar(...)` | Importe l'AAR de chaque module de `shared/` |
| `android/core/<nom>` | `android_module(...)` | Composants Compose communs |
| `android/feature/<nom>` | `android_module(...)` | Écrans Compose d'une feature |
| `android/app` | `android_application(...)` | Application Android, navigation |
| `ios/Core/<Nom>`, `ios/Features/<Nom>`, `ios/SharedKit` | `ios_module(...)` | Module Swift, aussi paquet Swift |
| `ios/App` | `ios_application(...)` | Application iOS, navigation |

## Règles

1. Aucune logique métier dans `android/` ni dans `ios/`. Un écran affiche un état et transmet des actions.
2. Une feature ne dépend jamais d'une autre feature. Ce qui est commun descend dans un module `core`.
3. Les dépendances entre modules suivent le tableau ci-dessous. Le build les refuse sinon : la tâche Gradle `checkModuleRules` pour `shared/`, la visibilité Bazel pour `android/` et `ios/`.
4. Les versions vivent dans `gradle/libs.versions.toml` et `MODULE.bazel`, jamais dans un module. Bazel importe toutes les `[libraries]` du catalogue et y lit les niveaux de SDK Android.
5. Les plugins Gradle tiers ne sont appliqués que par `build-logic/`. Ils sont déclarés dans `[plugins]` du catalogue, que Bazel ignore.
6. Ne jamais modifier à la main : les `Package.swift` des modules iOS, `maven_install.json`, `android/shared/maven/`, `ios/SharedKit/Frameworks/`, les projets Xcode.
7. Le code et ses commentaires sont en anglais. La documentation et les textes affichés à l'utilisateur sont en français.

Côté Android, le module partagé `shared/<chemin>` s'écrit `//android/shared:<chemin avec _>`, par exemple `//android/shared:feature_login`.

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
3. Pont vers Android : dans `android/shared/BUILD.bazel`, déclarer `shared_aar(name = "feature_<nom>", module = "feature/<nom>", deps = [...])` avec les dépendances de son `build.gradle.kts`, l'ajouter aux `deps` de `umbrella`, puis lancer `tools/shared/sync --android`.
4. Android : créer `android/feature/<nom>/BUILD.bazel` avec `android_module`, qui dépend de `//android/shared:feature_<nom>`, l'ajouter aux `deps` de `android/app/BUILD.bazel`, et brancher l'écran dans `AppRoot`.
5. iOS : créer `ios/Features/<Nom>/BUILD.bazel` avec `ios_module`, lancer `bazel run //ios/Features/<Nom>:package_swift`, ajouter le module aux dépendances de `ios/App`, ses tests à `ios/BUILD.bazel`, et brancher la vue dans `RootView`.

## Terminé veut dire

- Les contrôles de la zone touchée passent : `./gradlew check` pour `shared/` ; `tools/shared/sync --android` puis `bazel test //android/...` pour `android/` et pour tout changement de `shared/` ; `tools/shared/sync --ios` puis `bazel test //ios/...` pour `ios/` et pour tout changement de `shared/` visible depuis Swift.
- Les fichiers générés sont à jour, `maven_install.json` compris.
- Aucune règle de dépendance n'est enfreinte.
- Le comportement ajouté a un test, dans la couche où il vit.

## Ce qu'un agent peut vérifier

| Contrôle | Linux | macOS |
|---|---|---|
| Tests JVM du code partagé, règles de dépendance | Oui | Oui |
| AAR du code partagé, build et tests Android avec Bazel | Oui, avec le SDK Android | Oui |
| Framework `Shared` et tests Kotlin/Native | Non | Oui |
| Build et tests iOS avec Bazel, paquets Swift | Non | Oui |

Réécrire `maven_install.json` demande l'accès à Google Maven et à Maven Central. Un agent sur Linux ne peut pas valider un changement iOS. Il s'appuie sur la CI de la pull request et le dit dans sa description : ce qui a été vérifié, et ce qui ne l'a pas été.
