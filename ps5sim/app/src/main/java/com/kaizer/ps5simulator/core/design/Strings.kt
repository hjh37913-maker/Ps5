package com.kaizer.ps5simulator.core.design

/**
 * Localisation.
 *
 * The console ships its interface in many languages; we cover the three most requested
 * by this project's audience (English, Ukrainian, Russian). Keys are stable English
 * identifiers, lookups fall back to English so a missing string can never blank a label.
 */
object Strings {

    enum class Lang(val code: String, val label: String) {
        EN("en", "English"),
        UK("uk", "Українська"),
        RU("ru", "Русский");

        companion object {
            fun of(code: String): Lang = entries.firstOrNull { it.code == code } ?: EN
        }
    }

    private val en = mapOf(
        "games" to "Games", "media" to "Media",
        "must_see" to "Must see", "store" to "PlayStation Store",
        "game_library" to "Game Library", "library" to "Game Library",
        "console_storage" to "Console storage", "sort_by" to "Sort by",
        "search" to "Search", "all" to "All", "installed" to "Installed",
        "favourites" to "Favourites", "recent" to "Recent", "name" to "Name (A - Z)",
        "size" to "Size", "release" to "Release year",
        "play" to "Play", "resume" to "Resume", "jump_in" to "Jump in",
        "open" to "Open", "close" to "Close", "cancel" to "Cancel", "save" to "Save",
        "settings" to "Settings", "profile" to "Profile", "trophies" to "Trophies",
        "friends" to "Friends", "overview" to "Overview", "activity" to "Activity",
        "recently_played" to "Recently played", "trophy_level" to "Trophy level",
        "total" to "Total", "new_to_console" to "New to console",
        "time_to_beat" to "Time to beat", "your_time" to "Your time",
        "reviews" to "Reviews", "achievements" to "Achievements",
        "simulation" to "Simulation running", "close_simulation" to "Close simulation",
        "control_center" to "Control Center", "notifications" to "Notifications",
        "downloads" to "Downloads", "music" to "Music", "sound" to "Sound",
        "microphone" to "Microphone", "accessories" to "Accessories",
        "headset" to "Headset", "power" to "Power", "home" to "Home",
        "turn_off" to "Turn Off Console", "language" to "Language",
        "profile_name" to "Profile name", "start_tab" to "Start tab",
        "animations" to "Animations", "reduced_motion" to "Reduced motion",
        "dynamic_background" to "Dynamic background", "show_clock" to "Show clock",
        "show_hints" to "Show hints", "ui_scale" to "UI scale",
        "sfx" to "UI sound effects", "volume" to "Sound volume", "haptics" to "Haptics",
        "about" to "About this app", "reset" to "Reset preferences",
        "restore_defaults" to "Restore defaults", "featured" to "Featured",
        "deals" to "Deals for you", "add_library" to "Add to Library",
        "store_demo" to "Store is a visual demo — no purchases",
        "media_sub" to "Videos and music from your library",
        "art_notice" to "ORIGINAL CONCEPT ART · SIMULATOR",
        "settings_sub" to "Changes are saved to this device",
        "done" to "Done", "on" to "On", "off" to "Off",
        "virtual_pad" to "Virtual DualSense",
    )

    private val uk = mapOf(
        "games" to "Ігри", "media" to "Медіа",
        "must_see" to "Варто побачити", "store" to "PlayStation Store",
        "game_library" to "Бібліотека ігор", "library" to "Бібліотека ігор",
        "console_storage" to "Пам'ять консолі", "sort_by" to "Сортувати",
        "search" to "Пошук", "all" to "Усі", "installed" to "Встановлені",
        "favourites" to "Обрані", "recent" to "Нещодавні", "name" to "Назва (А - Я)",
        "size" to "Розмір", "release" to "Рік випуску",
        "play" to "Грати", "resume" to "Продовжити", "jump_in" to "Долучитися",
        "open" to "Відкрити", "close" to "Закрити", "cancel" to "Скасувати",
        "save" to "Зберегти", "settings" to "Налаштування", "profile" to "Профіль",
        "trophies" to "Нагороди", "friends" to "Друзі", "overview" to "Огляд",
        "activity" to "Активність", "recently_played" to "Нещодавно грали",
        "trophy_level" to "Рівень нагород", "total" to "Усього",
        "new_to_console" to "Нова для консолі", "time_to_beat" to "Час проходження",
        "your_time" to "Ваш час", "reviews" to "Відгуки", "achievements" to "Досягнення",
        "simulation" to "Симуляція запущена", "close_simulation" to "Закрити симуляцію",
        "control_center" to "Центр керування", "notifications" to "Сповіщення",
        "downloads" to "Завантаження", "music" to "Музика", "sound" to "Звук",
        "microphone" to "Мікрофон", "accessories" to "Аксесуари",
        "headset" to "Гарнітура", "power" to "Живлення", "home" to "Домівка",
        "turn_off" to "Вимкнути консоль", "language" to "Мова",
        "profile_name" to "Ім'я профілю", "start_tab" to "Початкова вкладка",
        "animations" to "Анімації", "reduced_motion" to "Менше руху",
        "dynamic_background" to "Динамічний фон", "show_clock" to "Показувати час",
        "show_hints" to "Підказки", "ui_scale" to "Масштаб інтерфейсу",
        "sfx" to "Звуки інтерфейсу", "volume" to "Гучність", "haptics" to "Вібрація",
        "about" to "Про застосунок", "reset" to "Скинути налаштування",
        "restore_defaults" to "Відновити типові", "featured" to "Рекомендовані",
        "deals" to "Пропозиції для вас", "add_library" to "Додати до бібліотеки",
        "store_demo" to "Магазин — лише демонстрація",
        "media_sub" to "Відео та музика з вашої бібліотеки",
        "art_notice" to "ОРИГІНАЛЬНИЙ КОНЦЕПТ-АРТ · СИМУЛЯТОР",
        "settings_sub" to "Зміни зберігаються на цьому пристрої",
        "done" to "Готово", "on" to "Увімк.", "off" to "Вимк.",
        "virtual_pad" to "Віртуальний DualSense",
    )

    private val ru = mapOf(
        "games" to "Игры", "media" to "Медиа", "must_see" to "Стоит увидеть",
        "store" to "PlayStation Store", "game_library" to "Библиотека игр",
        "library" to "Библиотека игр", "console_storage" to "Память консоли",
        "sort_by" to "Сортировать", "search" to "Поиск", "all" to "Все",
        "installed" to "Установленные", "favourites" to "Избранные",
        "recent" to "Недавние", "name" to "Название (А - Я)", "size" to "Размер",
        "release" to "Год выпуска", "play" to "Играть", "resume" to "Продолжить",
        "jump_in" to "Присоединиться", "open" to "Открыть", "close" to "Закрыть",
        "cancel" to "Отмена", "save" to "Сохранить", "settings" to "Настройки",
        "profile" to "Профиль", "trophies" to "Награды", "friends" to "Друзья",
        "overview" to "Обзор", "activity" to "Активность",
        "recently_played" to "Недавно играли", "trophy_level" to "Уровень наград",
        "total" to "Всего", "new_to_console" to "Новая для консоли",
        "time_to_beat" to "Время прохождения", "your_time" to "Ваше время",
        "reviews" to "Отзывы", "achievements" to "Достижения",
        "simulation" to "Симуляция запущена", "close_simulation" to "Закрыть симуляцию",
        "control_center" to "Центр управления", "notifications" to "Уведомления",
        "downloads" to "Загрузки", "music" to "Музыка", "sound" to "Звук",
        "microphone" to "Микрофон", "accessories" to "Аксессуары",
        "headset" to "Гарнитура", "power" to "Питание", "home" to "Главная",
        "turn_off" to "Выключить консоль", "language" to "Язык",
        "profile_name" to "Имя профиля", "start_tab" to "Начальная вкладка",
        "animations" to "Анимации", "reduced_motion" to "Меньше движения",
        "dynamic_background" to "Динамический фон", "show_clock" to "Показывать время",
        "show_hints" to "Подсказки", "ui_scale" to "Масштаб интерфейса",
        "sfx" to "Звуки интерфейса", "volume" to "Громкость", "haptics" to "Вибрация",
        "about" to "О приложении", "reset" to "Сбросить настройки",
        "restore_defaults" to "Восстановить по умолчанию", "featured" to "Рекомендуемые",
        "deals" to "Предложения для вас", "add_library" to "Добавить в библиотеку",
        "store_demo" to "Магазин — только демонстрация",
        "media_sub" to "Видео и музыка из вашей библиотеки",
        "art_notice" to "ОРИГИНАЛЬНЫЙ КОНЦЕПТ-АРТ · СИМУЛЯТОР",
        "settings_sub" to "Изменения сохраняются на этом устройстве",
        "done" to "Готово", "on" to "Вкл.", "off" to "Выкл.",
        "virtual_pad" to "Виртуальный DualSense",
    )

    fun get(lang: String, key: String): String = when (Lang.of(lang)) {
        Lang.UK -> uk[key] ?: en[key] ?: key
        Lang.RU -> ru[key] ?: en[key] ?: key
        Lang.EN -> en[key] ?: key
    }
}
