# Ассеты мобильных приложений

Персонажи новеллы, фоны сцен, иллюстрации сценариев, поезд hero и звезда в Android — не перерисовка,
а снимки **тех же React-компонентов сайта**. Поэтому приложение выглядит как сайт, а после правки иллюстрации на сайте
ассеты обновляются одной командой.

| Что | Откуда | Куда |
|-----|--------|------|
| `sprite_*` — персонажи | `Sprite` / `Person` (`frontend/src/story/StoryStage.tsx`, `components/illustrations.tsx`) | `android/app/src/main/res/drawable-nodpi` |
| `bg_*` — фоны сцен | `StoryBackground` (с теми же фильтрами: размытие, вечер, ночь) | там же |
| `scenario_*` — иллюстрации | `ScenarioImage` | там же |
| `hero_static`, `hero_parallax`, `hero_drive` — слои поезда | `HeroTrain`: неподвижный план, город (параллакс), сам поезд | там же |
| `avatar`, `star` | `Avatar`, `StarIcon` | там же |
| `photo_*` | `frontend/public/media` (датасет) | там же |

Имя спрайта: `sprite_<u|p>_<поза>_<эмоция>_<жест>_<предмет>_<l|r>` — `u` сотрудник в форме, `p` пассажир,
`l`/`r` — сторона сцены (справа персонаж повёрнут к центру). Приложение ищет точное сочетание, иначе базовое
(`…_<жест по эмоции>_none_l`, зеркально для правой стороны).

## Обновить

```bash
cd tools/mobile-assets && npm install && npx playwright install chromium && cd ../..
python tools/mobile-assets/combos.py > frontend/tools/sprites.json   # нужен PyYAML (есть в backend)
(cd frontend && npx vite --port 5174) &                               # страница frontend/tools/mobile-assets.html
node tools/mobile-assets/export.mjs                                   # нужен cwebp: brew install webp
```

## Шрифт и звуки

Manrope — Google Fonts, SIL Open Font License 1.1 (текст лицензии лежит рядом со шрифтами в приложениях).
Статические начертания 400/500/600/700/800 нарезаны из вариативного `Manrope[wght].ttf`:
`fontTools.varLib.instancer.instantiateVariableFont(font, {"wght": 700}, updateFontNames=True)`.
Музыка и вздохи — `frontend/public/audio`, как на сайте.
