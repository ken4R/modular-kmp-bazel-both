# modular-kmp-bazel-both

Projet mobile modulaire, iOS et Android : la logique est écrite une seule fois en Kotlin Multiplatform, l'interface reste native sur chaque plateforme (SwiftUI et Jetpack Compose).

Ce dépôt porte le prototype de la solution B : Gradle construit le code partagé et le publie en binaires, Bazel construit les deux applications.
