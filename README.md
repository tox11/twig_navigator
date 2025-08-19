# Twig Navigator (twig5)

Минимальный плагин для PhpStorm, улучшающий работу с шаблонами Twig.

## Требования
- PhpStorm 2024.1 – 2025.1 (build 241–253.*)
- JDK 17

## Как попробовать из исходников
- Сборка дистрибутива: `./gradlew buildPlugin`  
  Готовый ZIP появится в `build/distributions/`

## Установка собранного плагина
PhpStorm → Settings → Plugins → ⚙ → Install Plugin from Disk… → выбрать ZIP из `build/distributions/`.

## Технологии и совместимость
- Kotlin 2.0, Gradle
- Платформа: PhpStorm (`intellij.type = PS`)
- Зависимости: `com.jetbrains.php`, `com.jetbrains.twig`

## Лицензия
TBD