# android/ : application et écrans Compose

Interface Android en Jetpack Compose, construite par Bazel avec rules_android et rules_kotlin. Aucune logique métier ici : elle est dans `shared/`, que Bazel reçoit en AAR. Les règles communes du dépôt sont dans `AGENTS.md`, à la racine.

## Modules

- `app/` : assemble les features et possède la navigation (`AppRoot`). Macro `android_application`.
- `core/<nom>/` : composants Compose communs, comme `designsystem`. Macro `android_module`.
- `feature/<nom>/` : écrans d'une feature. Macro `android_module`.
- `shared/` : le code partagé vu d'Android, un AAR par module de `shared/`, importé par `shared_aar`.

Les macros vivent dans `android_module.bzl` et `shared_aar.bzl`. `BUILD.bazel` porte le compilateur Kotlin, le plugin Compose et le runtime de Robolectric. `catalog.bzl` lit les niveaux de SDK dans `gradle/libs.versions.toml`.

## Avant de construire

`android/shared/maven/` vient du code Kotlin et n'est pas versionné. `tools/shared/sync --android` le remplit. Sans lui, Bazel s'arrête et rappelle la commande. Après un changement sous `shared/`, relancer la commande : Bazel voit les nouveaux AAR et reconstruit ce qui en dépend.

## Déclarer un module

```starlark
load("//android:android_module.bzl", "android_module")

android_module(
    name = "login",
    deps = [
        "//android/core/designsystem",
        "//android/shared:feature_login",
    ],
)
```

- Le nom du module est le nom de son dossier. Son namespace, le paquet de sa classe `R`, en découle : `com.example.modular.feature.login`.
- Sources dans `src/main/kotlin/`, ressources dans `src/main/res/`, tests dans `src/test/kotlin/`. Chaque fichier `*Test.kt` devient une cible de test du même nom.
- La macro ajoute Compose, son compilateur, et pour une feature ce qu'il faut pour observer un ViewModel partagé. Une autre bibliothèque doit figurer dans `[libraries]` du catalogue, puis s'écrit `@maven//:<groupe>_<artefact>`, points et tirets remplacés par `_`.
- Une feature n'est visible que de `//android/app`. L'AAR d'une feature partagée n'est visible que de sa feature Android et de l'application, l'umbrella que de l'application : Bazel refuse le reste.
- Quand un module de `shared/` gagne une dépendance dans son `build.gradle.kts`, la reporter dans les `deps` de son `shared_aar` : un AAR ne déclare pas ses dépendances à Bazel.

## Écrire un écran

Le modèle est `feature/login`.

- `XxxRoute(viewModel, on…)` relie le ViewModel partagé à l'écran. Il collecte l'état avec `collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)`, pour qu'un champ de texte ne soit jamais en retard d'une frappe, et transmet les événements dans un `LaunchedEffect`.
- `XxxScreen(state, on…)` est un composable sans état, avec un `@Preview`. Il affiche l'état et remonte les actions.
- Les textes sont dans le `res/values/strings.xml` du module.
- L'application crée le ViewModel : `viewModel { graph.loginViewModel() }`.

## Tests

- Tests d'écran sur JVM, avec Robolectric et `createComposeRule()`, dans `src/test`. Voir `LoginScreenTest`, et `LoginRouteTest`, qui pilote le vrai ViewModel partagé chargé depuis son AAR.
- `bazel test //android/feature/login:all`, ou `bazel test //android/...` pour tout.
- Robolectric tourne hors ligne, avec le runtime Android du `targetSdk`, épinglé dans le catalogue (`robolectric-android-all`). Un test qui demande un autre niveau de SDK, par `@Config(sdk = ...)`, ne trouve pas son runtime.

## Différences avec le build Gradle du prototype A

- Pas de variantes : un seul APK de debug, signé avec la clé de debug de rules_android. `ui-tooling`, qui n'entrait que dans la variante debug, n'est pas inclus.
- Pas de lint Android dans le build.
- Android Studio lit le projet par son plugin Bazel, pas par la synchronisation Gradle. Ce prototype ne l'a pas encore essayé : les previews Compose n'y ont pas de support officiel, et le code partagé y apparaît comme un binaire, accompagné de ses sources.
