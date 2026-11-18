# android/ : application et écrans Compose

Interface Android en Jetpack Compose, construite par Gradle. Aucune logique métier ici : elle est dans `shared/`. Les règles communes du dépôt sont dans `AGENTS.md`, à la racine.

## Modules

- `app` : assemble les features et possède la navigation (`AppRoot`). Plugin `android.app`.
- `core/<nom>` : composants Compose communs, comme `designsystem`. Plugin `android.core`.
- `feature/<nom>` : écrans d'une feature. Plugin `android.feature`.

## Écrire un écran

Le modèle est `feature/login`.

- `XxxRoute(viewModel, on…)` relie le ViewModel partagé à l'écran. Il collecte l'état avec `collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)`, pour qu'un champ de texte ne soit jamais en retard d'une frappe, et transmet les événements dans un `LaunchedEffect`.
- `XxxScreen(state, on…)` est un composable sans état, avec un `@Preview`. Il affiche l'état et remonte les actions.
- Les textes sont dans le `res/values/strings.xml` du module.
- L'application crée le ViewModel : `viewModel { graph.loginViewModel() }`.

## Tests

- Tests d'écran sur JVM, avec Robolectric et `createComposeRule()`, dans `src/test`. Voir `LoginScreenTest`.
- `./gradlew :android:feature:login:testDebugUnitTest`.
