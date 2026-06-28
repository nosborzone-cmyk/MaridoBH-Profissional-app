# Build online

Este projeto foi preparado para GitHub Actions.

O workflow usa:

- Ubuntu latest
- Java 17
- Android SDK
- Gradle 8.7 baixado automaticamente pelo script `gradlew`
- Firebase Google Services Plugin 4.5.0
- Firebase BoM 34.15.0

Comando executado no servidor:

```bash
./gradlew :app:assembleDebug --stacktrace
```
