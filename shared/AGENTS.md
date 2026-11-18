# shared/ : logique commune en Kotlin Multiplatform

Tout ce que les deux applications ont en commun vit ici, et y est testé une seule fois. Construit par Gradle. Les règles communes du dépôt sont dans `AGENTS.md`, à la racine.

## Modules

- `core/<nom>` : brique commune (session, réseau, modèle). Plugin `kmp.core`.
- `feature/<nom>` : logique d'un écran ou d'un parcours. Plugin `kmp.feature`, qui ajoute le `ViewModel` multiplateforme d'AndroidX.
- `umbrella` : le seul module qui produit le framework iOS `Shared`. Plugin `kmp.umbrella`. `SharedGraph` y assemble les dépendances à la main, en attendant le choix d'une bibliothèque d'injection.

## Écrire la logique d'une feature

Le modèle est `feature/login`.

- Un `ViewModel` expose `state: StateFlow<XxxUiState>`, `events: Flow<XxxEvent>` et des fonctions d'action qui ne renvoient rien.
- `XxxUiState` est une `data class` immuable. Ses valeurs par défaut décrivent l'état initial.
- `XxxEvent` est une `sealed interface` d'intentions de navigation, livrées une seule fois. Le code partagé dit ce qui s'est passé, chaque application décide où aller.
- Un échec métier est une valeur (`sealed interface`, `enum`), pas une exception. Le texte affiché est choisi par chaque plateforme.
- Une feature dépend des interfaces de `core`, jamais d'une implémentation.

## Ce que Swift voit

- `explicitApi()` est actif : chaque déclaration donne sa visibilité, et ce qui est `public` est une API. Ne rendre public que ce dont un écran a besoin.
- Un module garde ses noms Kotlin en Swift s'il est passé à `exportToSwift(...)` dans `umbrella/build.gradle.kts`.
- SKIE traduit l'API : un `StateFlow` devient un `SkieSwiftStateFlow`, un `Flow` une séquence asynchrone, une fonction `suspend` une fonction `async throws`, une hiérarchie `sealed` un `switch` exhaustif avec `onEnum(of:)`, un `enum` Kotlin un `enum` Swift.
- Les valeurs par défaut des paramètres n'existent pas en Swift : il passe tous les arguments.
- Après un changement visible depuis Swift, relancer `tools/shared/sync` puis `bazel test //ios/...`, sur macOS ou par la CI.

## Tests

- Dans `commonTest`, avec `kotlin.test` et `kotlinx-coroutines-test`. Voir `LoginViewModelTest`.
- Sur JVM : `./gradlew :shared:feature:login:testAndroidHostTest`.
- Sur un simulateur iOS, macOS seulement : `./gradlew :shared:feature:login:iosSimulatorArm64Test`.
