# Correção aplicada: Firebase BoM / Kotlin

O erro do GitHub Actions indicava incompatibilidade de metadados Kotlin:

```text
was compiled with an incompatible version of Kotlin
metadata is 2.2.0, expected 1.9.0
Execution failed for task :app:compileDebugKotlin
```

Isso aconteceu porque o Firebase BoM anterior estava puxando bibliotecas compiladas com Kotlin mais novo do que o compilador usado no projeto.

## Ajustes feitos

- `com.google.gms.google-services`: 4.5.0 -> 4.4.2
- `firebase-bom`: 34.15.0 -> 33.7.0

Essas versões são mais estáveis para o conjunto atual:

- Android Gradle Plugin 8.5.2
- Kotlin 1.9.24
- Java 17
- GitHub Actions Ubuntu

## Próximo teste

Suba esta versão no GitHub e rode novamente o workflow **Android Debug APK**.
