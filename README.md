# LINC-Chat

Многофункциональный чат-плагин для Paper (с прицелом на порт на Spigot/Bukkit/Folia): каналы Global/Local/Trade/PM, интеграция с LuckPerms и Vault, настраиваемые цвета префикса/ника/сообщения, метки каналов, модерация, антиспам, JSON hover/click, item-link токены `*item1`–`*item9` и публичный API для сторонних плагинов.

Полное техническое задание — [`docs/TOR.md`](docs/TOR.md). Ниже — фактический статус реализации относительно этого ТЗ.

## Статус

Реализованы **Phase 0–2** полностью и ключевые фичи **Phase 3** (см. таблицу ниже). Проект собирается (`./gradlew build`) и готов к запуску на тестовом сервере. **Phase 4** (DiscordSRV, WorldGuard, chat bubbles, GUI-палитра, offline-почта, `/mutehistory`, `/chatlog`-книга, спойлер-теги, SQL-хранилище) вынесена за рамки текущей реализации, но архитектурно подготовлена (см. "Заделы на Phase 4").

### Что реализовано

| Область | Статус | Комментарий |
|---|---|---|
| Gradle-проект, `plugin.yml`, JDK 21, shadow-джар | ✅ | `build.gradle.kts`, relocate для Configurate/SnakeYAML |
| Абстракции интеграций (Permissions/Economy/Placeholder/Scheduler) | ✅ | `integration/` — задел под Folia и Spigot-порт |
| Конфиги (`config.yml`, `channels.yml`, `messages_ru.yml`, `messages_en.yml`) | ✅ | Configurate 4.x YAML, сохранение комментариев, автослияние новых ключей |
| Хранилище данных игрока | ✅ (YAML) | `playerdata/<uuid>.yml`; `SqlPlayerDataStore` — заглушка на Phase 4 |
| Каналы Global / Local / Trade / PM | ✅ | Индивидуальное вкл/выкл, шорткаты, права speak/see |
| Локальный чат: радиус, 2D/3D, per-world | ✅ | Дефолт 200 блоков, override через LuckPerms-мету/permission-ноды |
| Цвета: приоритет игрок → LuckPerms-мета → дефолт конфига | ✅ | `/chatcolor`, права `unichat.color.basic/hex/gradient` |
| LuckPerms / Vault / PlaceholderAPI интеграции | ✅ | Прямой LuckPerms API, live-обновление через `UserDataRecalculateEvent` |
| Модерация: мут, игнор, антиспам-cooldown, anti-caps, swear-filter, advertising-filter | ✅ | Раздел 11 ТЗ |
| `/socialspy`, chat-log в файл, `/chatpause`, `/slowmode` | ✅ | |
| PlaceholderAPI-экспаншен `%unichat_*%` | ✅ | |
| Item-link токены `*item1`–`*item9` + hover тултип предмета | ✅ | Раздел 12.6 ТЗ |
| Mentions (`@nick`) + звук + JSON hover/click по нику | ✅ | |
| `/me` | ✅ | |
| Команды | ✅ | Paper 1.21: Brigadier `LifecycleEvents.COMMANDS`. Paper 1.20.1: Bukkit `plugin.yml` + `CommandExecutor` |
| Публичный API (`UniChatAPI`, кастомные события) | ✅ | `api/` — для сторонних плагинов |
| DiscordSRV-мост, WorldGuard-ограничения, chat bubbles, GUI-палитра, offline-почта, `/mutehistory`, `/chatlog` (книга), спойлер-теги, SQL-бэкенд | ⏭️ Phase 4 | Точки расширения заложены (`ChatBridge`, `ChannelAccessGuard`, `SqlPlayerDataStore`) |

### Заделы на Phase 4

Чтобы не блокировать будущую доработку, в код уже заложены точки расширения:

- `integration/ChatBridge.java` — интерфейс для DiscordSRV/другого моста, пока не подключён к пайплайну.
- `integration/ChannelAccessGuard.java` — интерфейс для региональных ограничений (WorldGuard); `ChannelManager.setAccessGuard(...)` уже используется в проверке прав.
- `storage/sql/SqlPlayerDataStore.java` — заглушка `PlayerDataStore` для SQLite/MySQL; переключение бэкенда — через `storage.type` в `config.yml` (сейчас поддерживается только `yaml`, при другом значении плагин логирует предупреждение и использует YAML).
- Provider-абстракции (`PermissionsProvider`, `EconomyProvider`, `PlaceholderProvider`, `SchedulerProvider`) отделяют весь код от Bukkit-scheduler/Vault/LuckPerms API напрямую — облегчает будущий порт на Spigot и адаптацию под Folia.

## Критерии приёмки (DoD раздела 17 ТЗ)

- [x] Плагин собирается (`./gradlew build` зелёный).
- [ ] Проверено на чистом Paper 1.20.4+/1.21+ без LuckPerms/Vault/PAPI — ожидается работа через no-op провайдеры, но живой тест на сервере ещё не проводился.
- [ ] Проверено с LuckPerms + Vault на живом сервере (live-обновление меты без релога).
- [x] Все 4 канала (Global/Local/Trade/PM) реализованы, независимо включаются/выключаются в `channels.yml`.
- [x] Радиус локального чата настраивается в конфиге и применяется через `/unichat reload` без перезапуска.
- [x] Метки `[G]`/`[L]`/`[$]`/`[ЛС]` настраиваются в `channels.yml` (текст + цвет через MiniMessage-теги).
- [x] `/chatcolor` сохраняет цвет между заходами (YAML playerdata).
- [x] Мут/игнор/антиспам-cooldown реализованы и блокируют превышение лимитов.
- [x] PlaceholderAPI-экспаншен регистрируется при наличии PAPI.
- [x] `*item1`…`*item9` заменяются на предмет из хотбара с hover-тултипом; пустой слот/нет прав обрабатываются по конфигу (`item-link.*`).
- [ ] Финальная проверка "нет дублирования сообщений / исключений в логах" — требует живого теста на сервере (join/quit/chat/reload).

Пункты без ✅ требуют разворачивания на реальном Paper-сервере — статический анализ и сборка этого не покрывают.

## Сборка

Требования: JDK 21. Сборка 1.20.1 компилируется с `--release 17` (байткод Java 17), сборка 1.21 — Java 21.

```powershell
# Windows PowerShell, из корня проекта — обе сборки
.\gradlew.bat build
```

```bash
# Linux/macOS
./gradlew build
```

Готовые shadow-джары (Configurate/SnakeYAML уже внутри):

| Сервер | Артефакт |
|---|---|
| Paper **1.21+** (Java 21) | `build/libs/linc-chat-<version>.jar` |
| Paper **1.20.1** (Java 17+) | `paper-1.20.1/build/libs/LINC-Chat-1.20.1-<version>.jar` |

Только 1.20.1: `.\gradlew.bat :paper-1.20.1:build`

Не ставьте оба jar на один сервер — это один и тот же плагин `LINC-Chat`.

## Установка

1. Скопируйте нужный jar в папку `plugins/` Paper-сервера (**1.21+** или **1.20.1** — см. таблицу выше).
2. (Опционально, но рекомендуется) поставьте LuckPerms и Vault для полноценных цветов/префиксов и экономики Trade-канала; PlaceholderAPI — для `%unichat_*%` и сторонних плейсхолдеров в форматах каналов.
3. Запустите сервер. При первом старте создадутся `plugins/LINC-Chat/config.yml`, `channels.yml`, `messages_ru.yml`, `messages_en.yml`.
4. Настройте `channels.yml`/`config.yml` по вкусу, затем `/unichat reload` (право `unichat.admin.reload`) — без перезапуска сервера.
5. `/unichat debug` (право `unichat.admin.debug`) выводит текущий статус интеграций, число каналов и закэшированных профилей — полезно для диагностики после установки.

## Основные команды

| Команда | Описание |
|---|---|
| `/ch <channel>`, `/ch toggle <channel>` | Переключить активный канал / вкл-выкл прослушивание |
| `/msg`, `/tell`, `/w`, `/r` | Приватные сообщения и ответ на последнее |
| `/ignore`, `/unignore` | Игнор-лист |
| `/socialspy` | Просмотр чужих ЛС (для персонала) |
| `/chatcolor` | Личные цвета префикса/ника/сообщения |
| `/mute`, `/unmute` | Мут (глобальный или по каналу, с TTL) |
| `/chatclear` | Очистка чата |
| `/chatpause`, `/slowmode` | Заморозка чата / минимальный кулдаун |
| `/me` | Ролевая реплика от третьего лица |
| `/unichat reload\|debug` | Администрирование плагина |

Полный список прав — в `plugin.yml`.

## Структура проекта

```
src/main/java/com/leidcool/lincchat/
├── LincChatPlugin.java     — bootstrap: DI всех сервисов, onEnable/onDisable
├── api/                    — публичный API + кастомные события для сторонних плагинов
├── channel/                — модель каналов (Global/Local/Trade/PM/Custom)
├── color/                  — резолвер цветов и permission-политика
├── commands/               — команды (Paper Brigadier)
├── config/                 — Configurate-обёртки над config.yml/channels.yml/messages
├── core/                   — реализация публичного API
├── format/                 — FormatEngine, item-link/mention парсеры
├── integration/            — Permissions/Economy/Placeholder/Scheduler-провайдеры
│   ├── luckperms/, vault/, placeholderapi/, paper/
├── listener/               — ChatListener и join/quit/permission-listener'ы
├── moderation/             — мут, игнор, антиспам, фильтры, chat-log, pause/slowmode
├── storage/                — PlayerDataStore (YAML), кэш профилей, SQL-заглушка
└── util/                   — конвертер legacy-цветов, форматтер длительности
```

## Дальнейшие шаги

1. Развернуть на тестовом Paper-сервере и пройти чек-лист DoD выше вживую.
2. Реализовать Phase 4: DiscordSRV-мост, WorldGuard-ограничения каналов, chat bubbles, GUI-палитра цветов, offline-почта, `/mutehistory`, `/chatlog` (книга), спойлер-теги, SQLite/MySQL storage.
