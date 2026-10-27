# RustLauncher-Authlib

Патченные сборки `com.mojang:authlib` для Rust лаунчера от **Luxinfine**.

Каждая папка `Authlib-*` — самодостаточный Gradle-проект. При сборке исходники
из папки (патч) компилируются `compileOnly`-поверх оригинального
`com.mojang:authlib:<версия>`, затем мёржатся с оригинальным jar. Плагин
JarSplitter делит результат на клиентскую и серверную версии в
`build/libs/authlib-<версия>-client.jar` и
`build/libs/authlib-<версия>-server.jar`. Классы патча имеют те же FQCN, что и
оригинал, поэтому подменяют его в рантайме, переадресуя аутентификацию в API
лаунчера:

* клиентская сторона — `team.luxinfine.authlib.NativeAPI` (через
  `Invoke.invokeClient(...)`);
* серверная сторона — `team.luxinfine.authlib.ServerAPI` (через
  `Invoke.invokeServer(...)`).

Проект собирается одинаково во всех папках:

```bash
cd Authlib-<версия> && ./gradlew build
```

## Общая сборка и релиз

Сборка всех артефактов. Jar-ники будут в папке `builds/`
```bash
./gradlew buildAll
```

Сборка и публикация артефактов в Github Release:
```bash
./gradlew githubRelease
```

## Запуск сервера

Запустите серверное ядро, подключив `authlib-version-server.jar` как javaagent с указанием настроек через jvm аргументы:
```bash
java -javaagent:authlib-version-server.jar \
  -DLFLauncher.Server.ApiUrl=http://localhost:9274/api \
  -DLFLauncher.Server.AccessToken=token \
  -jar paper.jar
```

Параметры `-D` должны идти перед `-jar`:
* `LFLauncher.Server.ApiUrl` — базовый URL API лаунчера.
* `LFLauncher.Server.AccessToken` — токен доступа к API. Скопируйте из связанного с клиентом .hocon профиля в лаунчер-сервере

## Версии

Диапазон версий игры → модуль с кодом патча для нужной версии authlib

| Диапазон версий Minecraft | Модуль с кодом                     | authlib |
|---------------------------|------------------------------------|---------|
| 1.0 – 1.6.4               | [Authlib-legacy](Authlib-legacy)   | legacy  |
| 1.7.2                     | [Authlib-1_3](Authlib-1_3)         | 1.3     |
| 1.7.3 – 1.7.4             | [Authlib-1_2](Authlib-1_2)         | 1.2     |
| 1.7.5                     | [Authlib-1_3_1](Authlib-1_3_1)     | 1.3.1   |
| 1.7.6 – 1.7.7             | [Authlib-1_5_8](Authlib-1_5_8)     | 1.5.8   |
| 1.7.8                     | [Authlib-1_5_12](Authlib-1_5_12)   | 1.5.12  |
| 1.7.9                     | [Authlib-1_5_13](Authlib-1_5_13)   | 1.5.13  |
| 1.7.10 – 1.10.2           | [Authlib-1_5_22](Authlib-1_5_22)   | 1.5.22  |
| 1.11 – 1.15.2             | [Authlib-1_5_24](Authlib-1_5_24)   | 1.5.24  |
| 1.16 – 1.16.3             | [Authlib-1_6_25](Authlib-1_6_25)   | 1.6.25  |
| 1.16.4 – 1.16.5           | [Authlib-2_1_28](Authlib-2_1_28)   | 2.1.28  |
| 1.17 – 1.17.1             | [Authlib-2_3_31](Authlib-2_3_31)   | 2.3.31  |
| 1.18 – 1.18.2             | [Authlib-3_2_38](Authlib-3_2_38)   | 3.2.38  |
| 1.19                      | [Authlib-3_5_41](Authlib-3_5_41)   | 3.5.41  |
| 1.19.1 – 1.19.2           | [Authlib-3_11_49](Authlib-3_11_49) | 3.11.49 |
| 1.19.3                    | [Authlib-3_16_29](Authlib-3_16_29) | 3.16.29 |
| 1.19.4                    | [Authlib-3_18_38](Authlib-3_18_38) | 3.18.38 |
| 1.20 – 1.20.1             | [Authlib-4_0_43](Authlib-4_0_43)   | 4.0.43  |
| 1.20.2                    | [Authlib-5_0_47](Authlib-5_0_47)   | 5.0.47  |
| 1.20.3 – 1.21.8           | [Authlib-6_0_58](Authlib-6_0_58)   | 6.0.58  |
| 1.21.9 – 26.1.2           | [Authlib-7_0_61](Authlib-7_0_61)   | 7.0.61  |
| 26.2                      | [Authlib-9_0_75](Authlib-9_0_75)   | 9.0.75  |
| 26.3                      | [Authlib-10_0_77](Authlib-10_0_77) | 10.0.77 |

#### Legacy верии игры и Authlib

Authlib-legacy - это кастомная сборка мини-версии authlib для тех версий игры, где authlib вообще отсутствовал, а
авторизация выполнялась через хардкод запросы к session.minecraft.net внутри кода игры. Кастомная версия устанавливает
глобальный хук URL для перенаправления запросов к mojang API на API лаунчера.

Для версий игры, где уже существует authlib, но без поддержки Textures API - также устанавливается глобальный перехват,
для skins.minecraft.net.

## Формат Properties в GameProfile

Authlib записывает SKIN, CAPE, ELYTRA параметры в формат mojang. В качестве сигнатуры проперти всегда задается пустая строка.
Все проверки сигнатуры в коде подменены на возврат true без условий.

## Как загружается Authlib

### Клиент

* `ClientEntrypoint` запускается системным загрузчиком (`AppClassLoader`) и через него передаёт управление главному
  классу игры. Это позволяет использовать одну точку входа до запуска vanilla-клиента или загрузчика модификаций.
* Патченные `com.mojang.authlib.*` и связанные с ними `team.luxinfine.authlib.*` загружаются загрузчиком игры/модлоадера
  из клиентского JAR. Они должны находиться в этом загрузчике, чтобы видеть зависимости и типы самой игры.
* `team.luxinfine.rslauncher.client.NativeAPI` определяет сам лаунчер через JNI в bootstrap-загрузчике до запуска игры.
  Поэтому этот класс виден загрузчику игры и модлоадерам через делегирование родителю; в authlib JAR он намеренно
  исключён.

### Сервер

* Серверный JAR запускается как Java agent: `java -javaagent:authlib-version-server.jar -jar server.jar`. JVM вызывает
  `ServerEntrypoint.premain` до `main` ядра. Agent не запускает ядро сам.
* В `premain` собирается временный JAR с support-классами `team.luxinfine.authlib.*` (кроме `boot`) и
  `team.luxinfine.rslauncher.*`; он добавляется через `Instrumentation.appendToBootstrapClassLoaderSearch`.
  Поэтому `CommonAPI`, `ServerAPI` и серверные вспомогательные классы видны любому загрузчику ядра и плагинов/модов.
  Временный файл удаляется при завершении JVM.
* `AuthlibTransformer` перехватывает загрузку `com.mojang.authlib.*` и нужных `team.luxinfine.*` классов и возвращает
  байткод из server JAR. Сами `com.mojang.authlib.*` определяет запросивший их загрузчик ядра, а не bootstrap:
  так они сохраняют доступ к библиотекам ядра (например, Gson), которых нет в bootstrap-загрузчике.
* Такое разделение важно из-за модели JVM: класс виден не только по имени, но и по загрузчику. Общие support-классы
  размещены в bootstrap для единой копии между загрузчиками, а Authlib — в загрузчике ядра для разрешения его
  зависимостей.