# ios/ : application et vues SwiftUI

Interface iOS en SwiftUI, construite par Bazel. Chaque module est aussi un paquet Swift, qui s'ouvre seul dans Xcode. Aucune logique métier ici : elle est dans `shared/`. Les règles communes du dépôt sont dans `AGENTS.md`, à la racine.

## Modules

- `App/` : assemble les features et possède la navigation (`RootView`).
- `SharedKit/` : le framework Kotlin `Shared` et les adaptateurs Swift, dont `Observed`. Un module écrit `import SharedKit`, jamais `import Shared`.
- `Core/<Nom>/` : composants SwiftUI communs, comme `DesignSystem`.
- `Features/<Nom>/` : vues d'une feature.

## Avant de construire

`ios/SharedKit/Frameworks/Shared.xcframework` vient du code Kotlin et n'est pas versionné. `tools/shared/sync` le construit. Sans lui, Bazel s'arrête et rappelle la commande.

## Déclarer un module

```starlark
load("//ios:ios_module.bzl", "ios_module")

ios_module(
    name = "Login",
    deps = [
        "//ios/Core/DesignSystem",
        "//ios/SharedKit",
    ],
)
```

- Le nom du module est le nom de son dossier.
- Sources dans `Sources/<Nom>/`, tests dans `Tests/<Nom>Tests/`.
- `BUILD.bazel` fait foi. `Package.swift` en est généré : `bazel run //ios/Features/<Nom>:package_swift`. `bazel test` échoue quand il n'est plus à jour.
- Une feature n'est visible que de `//ios/App` : Bazel refuse qu'une feature en importe une autre.
- Les bibliothèques Swift se construisent pour iOS seulement. Sur le Mac, `bazel test //ios/...` les construit à travers l'application et les tests, jamais pour macOS. Une `swift_library` écrite à la main, comme celle de `App`, porte `target_compatible_with = IOS_ONLY`.

## Écrire une vue

Le modèle est `Features/Login`.

- `XxxView`, publique, possède le ViewModel partagé à travers `Observed`. Elle le crée dans `.task`, parce que SwiftUI reconstruit la structure souvent. Elle transmet les événements à l'application avec `onEnum(of:)`.
- `XxxForm`, interne, est une vue sans état avec ses `#Preview`. Elle affiche l'état et remonte les actions.
- Une action passe par `model.send { $0.action() }` : l'état est relu aussitôt, et un champ de texte n'est jamais en retard d'une frappe.
- Les textes sont pour l'instant écrits dans les vues. Les ressources et la localisation des modules iOS restent à décider.

## Tests

- XCTest, dans `Tests/<Nom>Tests/`. Ils pilotent le vrai ViewModel partagé : voir `LoginViewTests`.
- `bazel test //ios/Features/Login:LoginTests`, ou `bazel test //ios/...` pour tout.
- Les tests tournent dans un simulateur réutilisé d'un lancement à l'autre, `BAZEL_TEST_<appareil>_<version d'iOS>`. Bazel le crée au premier lancement, et son premier démarrage peut prendre plusieurs minutes. L'appareil se choisit avec `--@rules_apple//apple/build_settings:ios_simulator_device="iPhone 17"`.

## Xcode

- Toute l'application : `bazel run //ios:xcodeproj`, puis ouvrir `ios/Modular.xcodeproj`. Xcode délègue le build à Bazel.
- Un seul module : ouvrir son `Package.swift`.
