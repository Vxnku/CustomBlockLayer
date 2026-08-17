# CustomBlockLayer — Master Agent & Developer Guide

## 📌 Общая Архитектура Проекта (Overview)
**CustomBlockLayer (CBL)** — комплексная система кастомных блоков без сущностей (Entity-less), работающая на Minecraft 1.21.1.
Проект разделен на две части:
1. **Client Mod (Fabric 1.21.1):** Отвечает за рендеринг кастомных текстур, динамические модели (FRAPI + Sodium), вкладки в Креатив-меню, MMB (Pick-Block) и парсинг ресурспаков.
2. **Server Plugin (Paper 1.21.1):** Отвечает за постоянное хранилище блоков в чанках (PDC), сетевую синхронизацию в реальном времени, интеграцию с WorldEdit 7.3, физику, ломание и дроп предметов.

---

## 🗂️ Структура Каталогов и Путей (Workspaces & Directories)

### 1. Исходный код (Repositories)
* **Клиентский мод (Fabric):** `F:\.Repos\Client+Server_Projects\CustomBlockLayer_ModFabric\`
* **Серверный плагин (Paper):** `F:\.Repos\Client+Server_Projects\CustomBlockLayer_PluginPaper\`
* **Основная папка Ресурспака:** `F:\.Repos\Client+Server_Projects\[RP][HG]CustomBlockLayer Content\`

### 2. Тестовое Окружение (Test Environment)
* **Тестовый сервер (Paper 1.21.1):** `A:\.TestServer1.21.1\`
  * Папка плагинов: `A:\.TestServer1.21.1\plugins\`
  * Папка ресурспака на сервере: `A:\.TestServer1.21.1\plugins\CustomBlockLayer\resourcepack\`
* **Клиент MultiMC (Test Instance):** `A:\MultiMC\instances\TEST2\.minecraft\`
  * Папка модов: `A:\MultiMC\instances\TEST2\.minecraft\mods\`
  * Папка ресурспаков: `A:\MultiMC\instances\TEST2\.minecraft\resourcepacks\[RP][HG]CustomBlockLayer Content\`

---

## ⚙️ Сборка и Деплой (Build & Deploy Instructions)

### Серверный плагин (Maven):
* **Команда сборки:**
  ```powershell
  & 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' clean package
  ```
* **Правило версии:** Перед каждой сборкой повышать версию в `pom.xml` (например, `0.0.15` -> `0.0.16`).
* **Деплой:** Удалить старый `customblocklayer*.jar` из `A:\.TestServer1.21.1\plugins\` и скопировать туда новый `target/customblocklayer-0.0.X.jar`.

### Клиентский мод (Gradle):
* **Команда сборки:**
  ```powershell
  Set-Location 'F:\.Repos\Client+Server_Projects\CustomBlockLayer_ModFabric'
  .\gradlew.bat classes; .\gradlew.bat build
  ```
* **Деплой:** Скопировать собранный `.jar` из `build/libs/customblocklayer-0.1.jar` в `A:\MultiMC\instances\TEST2\.minecraft\mods\customblocklayer-0.1.jar`.

---

## 🚨 КРИТИЧЕСКОЕ ПРАВИЛО: Работа с Ресурспаком (Resource Pack Sync Rule)
При любом добавлении, изменении или удалении файлов в основной папке ресурспака (`F:\.Repos\Client+Server_Projects\[RP][HG]CustomBlockLayer Content\`):
**ОБЯЗАТЕЛЬНО И АВТОМАТИЧЕСКИ синхронизировать файлы во все целевые папки:**
1. `A:\MultiMC\instances\TEST2\.minecraft\resourcepacks\[RP][HG]CustomBlockLayer Content\` *(клиент TEST2)*
2. `A:\.TestServer1.21.1\plugins\CustomBlockLayer\resourcepack\` *(сервер Paper)*
3. `F:\.Repos\Client+Server_Projects\CustomBlockLayer_PluginPaper\[RP][HG]CustomBlockLayer Content\` *(резервная копия)*

---

## 📄 Спецификация формата `.properties` (Block Definitions)
Файлы конфигурации блоков размещаются в `assets/minecraft/cbl/` или `assets/customblocklayer/cbl/`.

```properties
type = block
data = <custom_id>
name = <Отображаемое Имя в игре на Русском>
colum = <Название Вкладки в Креатив-меню>
matchBlocks = <minecraft_block_id_1>, <minecraft_block_id_2>
texture = <путь_к_текстуре>
texture.top = <текстура_верхней_грани>
texture.bottom = <текстура_нижней_грани>
texture.side = <текстура_боковых_граней>
```

### 1. Правила параметров:
* `name`: Отображаемое имя (русский язык, заглавные буквы, например: `Ступеньки из Красного бетона`).
* `colum`: Название вкладки (`Цветные Блоки`, `Анимации`, `Растения`, `Декор`). Все блоки также автоматически доступны в главной вкладке `CustomBlockLayer`.
* `matchBlocks`: Блоки, на которые можно наложить этот кастомный слой. Бетонные ступеньки/плиты ставятся **строго** на `quartz_stairs` и `quartz_slab` (non-smoothed).

---

## 🌿 Растения и Двухблочные структуры (Plants & Flowers)

### 1. Одиночные растения:
* Используют крестообразную модель (`cross`), слой `Cutout` с прозрачностью.
* В `QuadTransformer` сбрасывается `tintIndex = -1`, чтобы цвет цветка не затемнялся биомом.

### 2. Двухблочные растения (`TallPlantBlock`):
* В `.properties` указываются `texture.top` (бутон) и `texture.bottom` (стебель).
* Клиентский `RetexturedBakedModel` автоматически считывает `DoubleBlockHalf.UPPER` / `LOWER`.
* Серверный `BlockPlacementListener` при установке регистрирует ID сразу в нижний и верхний блоки (`y` и `y+1`). При ломании `BlockBreakListener` очищает оба блока и дропает ровно 1 предмет.

---

## ✨ Анимации текстур (`.mcmeta`)
* Текстура представляет собой вертикальную ленту кадров (например, 16x144 для 9 кадров 16x16).
* Рядом кладется файл `<имя_текстуры>.png.mcmeta`:
  ```json
  {
    "animation": {
      "frametime": 20,
      "interpolate": true
    }
  }
  ```
* `interpolate: true` дает плавный морфинг, `interpolate: false` — резкую ступенчатую/дерганую анимацию.
* Текстуры автоматически сшиваются в ванильный Block Atlas (`blocks.png`) и обновляются на GPU без пересборки чанков (0% нагрузки на процессор, полная совместимость с Sodium!).

---

## 🪄 Интеграция с WorldEdit 7.3 (WorldEdit Integration)

1. **Паттерны (`CBLPattern`):**
   * Поддерживает `//set cbl:<id>`, `//walls cbl:<id>`, `//cyl`, `//brush` и т.д. с автодополнением по `Tab`.
2. **Маски (`CBLMask`):**
   * Поддерживает `//replace cbl:<id>` и фильтрацию по кастомным блокам.
3. **Пространственное клонирование выделения (`Selection-Aware Spatial Cloning`):**
   * При `//stack`, `//move`, `//copy` и `//paste` перехватчик `CBLExtent` определяет смещение блока относительно выделения (`minPoint` / `maxPoint`) через `Math.floorMod`.
   * Кастомный слой мгновенно переносится на все клонированные координаты, сохраняется в PDC чанка и рассылается игрокам без перезахода на сервер!

---

## 🎁 Стандарты NBT и Предметов (Item & Drop Unification)
Все предметы (из Creative Tab, по клику MMB и при выпадении в Выживании) **100% идентичны по байтам**:
1. **NBT Компонент:** `PublicBukkitValues` со строками `customblocklayer:cbl` и `minecraft:cbl`.
2. **Display Name:** Желтый цвет (`NamedTextColor.YELLOW`), без курсива (`italic = false`).
3. **Базовый предмет:** Соответствует фактическому блоку в мире (`event.getBlock().getType()`).
4. Все предметы объединяются в один стак в инвентаре.
