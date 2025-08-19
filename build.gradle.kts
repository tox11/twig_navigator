import org.jetbrains.intellij.tasks.RunIdeTask

plugins {
    id("java")
    // Используем актуальную версию Kotlin
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.twig5"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

// ЕДИНЫЙ блок для настройки Java 17
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Стандартная библиотека Kotlin
    implementation(kotlin("stdlib"))

    // Зависимости для тестирования
    testImplementation("junit:junit:4.13.2")
    // Используем версию Kotlin из плагина для консистентности
    testImplementation(kotlin("test-junit"))
}

// Настраиваем платформу IntelliJ
intellij {
    // Версия SDK, с которой мы компилируем плагин. 2024.1 - это сборка 241.
    version.set("2024.1.4")

    // Указываем тип IDE. Ваша ошибка показывала "IU-...", что значит IntelliJ IDEA Ultimate.
    // Если вы работаете в PhpStorm, измените на "PS".
    type.set("PS")

    // Включаем скачивание исходников
    downloadSources.set(true)

    // Мы будем управлять версиями вручную в patchPluginXml, поэтому отключаем автоматику
    updateSinceUntilBuild.set(false)

    // Объявляем зависимости от плагинов PHP и Twig
    plugins.set(listOf("com.jetbrains.php", "com.jetbrains.twig"))
}

// ЕДИНЫЙ И ПРАВИЛЬНЫЙ БЛОК ДЛЯ ВСЕХ ЗАДАЧ
tasks {
    // Настраиваем JVM-совместимость
    withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }

    // Настраиваем опции компилятора Kotlin
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            // Обновляем версии API и языка для соответствия версии плагина Kotlin
            apiVersion = "2.0"
            languageVersion = "2.0"
        }
    }

    // Настраиваем plugin.xml с ПРАВИЛЬНЫМИ версиями
    patchPluginXml {
        version.set(project.version.toString())

        // ЭТОТ ДИАПАЗОН СОВМЕСТИМ С ВАШЕЙ IDE (251.*)
        sinceBuild.set("241") // Начиная с версии 2024.1
        untilBuild.set("253.*") // И до всех версий 2025.1

        // Задаём описание изменений
        changeNotes.set("""
            - Initial version of the Twig Navigator plugin.
            - Fixed compatibility issues for IDE versions 2024.1 and newer.
        """.trimIndent())
    }

    // Настраиваем задачу для запуска IDE в режиме отладки
    named<RunIdeTask>("runIde") {
        autoReloadPlugins.set(true)
        jvmArgs("-Xmx1024m")
    }

    // Отключаем индексацию опций для ускорения сборки
    buildSearchableOptions {
        enabled = false
    }
}