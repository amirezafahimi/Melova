# Melova standards

Check every change against this list before calling it done. If a change needs to break a
rule, say so and update this file in the same change.

## Language and wording

- Student and teacher pages are Persian. Never «سرور», a model name, an error code or a raw
  exception: say «توسط برنامه» or nothing. English labels already on a page (such as `T`, `V`,
  `G`, `s` and the audio format line) stay as they are unless the user asks to change them.
- The same goes for text the backend sends (`detail=`, failure reasons).
- A lesson has no subtitle: that field is «نام خواننده», optional, shown as a «خواننده: …» tag.
  Its stored name stays `subtitle` so old lessons keep it.
- «جستجو», not «کاوش». «گفتگو» for chats. «تحریر», «ویبراتو», «گذر» for the three ornaments.
- No guest concept on any page. Someone who hasn't signed in sees «ورود یا ثبت‌نام», and never
  sees followed teachers.
- Code comments and identifiers are English. Persian in a comment only quotes page text («…»).
- Comments say why, in plain sentences. Match the amount of comment around them.

## Pages (Compose)

- Colours come from `ui/theme`. Borders and dividers use `MelovaOutline` as it is, never faded with `.copy(alpha)`: a faded one vanishes on the light theme. A literal `Color(0x…)` is only for a third party's brand (Google
  button) and the lesson cover gradients.
- Solid accent fill takes white content; `MelovaAccentTint` takes `MelovaAccentSoft`, and
  `MelovaSecondaryTint` takes `MelovaSecondary`. A page keeps a solid fill for its one main
  action (record, upload, send); the media and side buttons around it take a tint.
- Reuse before writing: `MelovaSurfaceCard`, `MelovaTopBar`, `MelovaNavRow` for a row that opens
  something, `SectionHeader`, `ConfirmDialog`, `MelovaBackground`, `RtlContent`.
- A new page is `MelovaBackground { RtlContent { LazyColumn … } }` with system bar insets and
  `PagePadding`, and gets a route in `AppScreen` and a `composable` in `MelovaApp`.
- Back does what the page's back button does, on every page, from Android's system back and the
  browser's back alike. `MelovaTopBar`, `MelovaDialog` and sheets go through
  `PlatformBackHandler`; a new layer on a page (dialog, sheet, step) that closes with a button
  uses it too. A page's own back logic goes in the top bar's `onBack`, never only in the button.
- Sizes come from `MelovaDimens` (`ui/theme/Dimens.kt`): corners are only 12 (`MelovaSmallShape`:
  buttons, chips' boxes, inner panels), 20 (`MelovaCardShape`: cards) or a pill; buttons are 52
  for the page's one main action, 48 for the others, 40 for compact and icon buttons; gaps and
  padding step by 4 (4, 8, 12, 16, 24). Use space to group, not to fill: one card per task, the
  main action without scrolling, help text folded to one line until opened.
- A row in a list (lesson, section, teacher, chat) is one `MelovaSurfaceCard` with 12 padding: a
  `MelovaDimens.ListLead` (40) picture or badge, a `titleSmall` title and one `bodySmall` line,
  and its status at the end (progress ring, chip, arrow), never a third line. Rows sit 8 apart.
- Rows that open settings or pages and belong together (Profile, Settings, FAQ topics) share one
  card, split by thin lines: `MelovaNavGroup` with `MelovaGroupRow`, the icon in a tinted 32
  square. A row alone uses `MelovaNavRow`, which draws the same way.
- A dialog is for a question or two short fields. A form with an upload, a recording or several
  fields is its own page: numbered steps in cards, the one action pinned under them.
- A page that leads somewhere says where once: one main button for the next step (the next
  section, the next lesson), and the item it opens marked `highlighted` with a «بعدی» chip.
- Works at phone width in both themes. Anything shown over a photo or accent stays readable.
- A page's sides are 16 and its top 8 (`PagePadding`); the top bar closes with a thin line, so what follows starts below an edge.
- Nothing sits on the bottom edge of the screen. Whatever is pinned to the bottom of a page (a
  message box, buttons, the tab bar) keeps `PageBottomGap` under it, and a scrolling page ends
  with it through `PagePadding`. System bar insets don't count: they are zero in the browser.
- Every digit shows in Persian, typed ones too: the IRANSans files draw English digit code points
  as Persian. The text itself keeps what was typed, so a phone or email is sent as written.

## Shared code

- Screens and view models live in `commonMain`. A platform difference goes behind
  `expect`/`actual` in `platform/`; browser calls go through `MelovaAudioJs` to `melova-audio.js`.
- Every request goes through `apiCall`, or catches `Throwable`: a failed request in the browser
  isn't an `Exception`, and its English text would reach the page.
- No `!!` in new code, no `println`, no secrets or test values in code.
- State lives in the view model's `UiState`; screens get values and callbacks.
- Compile all three before calling it done, from the outer repo:
  `./gradlew :shared:compileDebugKotlinAndroid :shared:compileKotlinJs :shared:compileKotlinWasmJs`.

## Backend

- Routes in `api/`, logic and data in `melova_scoring/`. `student_store` is the only code that
  touches SQLite; tables are added to `_SCHEMA` with `CREATE TABLE IF NOT EXISTS`.
- One uvicorn worker: the sockets and in-process state depend on it.
- A new endpoint checks who is asking (`require_student`, `_who`, `_require_lesson_access`). The
  one exception is the labelling panel from loopback with `MELOVA_LABEL_OPEN=1`, which only
  `run_server.sh` sets; never set it on the server.
- Secrets live in `deploy/melova-api.env`, never in the example file, code or logs. Never turn on
  `MELOVA_LOGIN_LOG_CODES` on the server.
- New behaviour gets a test in `tests/`. Tests name the behaviour, as a sentence.
- A new dependency goes in `requirements.txt`; `update_server.sh` installs it on deploy.

## Before saying done

1. Run it: backend tests, and the changed page in the browser, at phone width.
2. Check nothing else moved: the path before and after your change, signed in and signed out.
3. Report what was and wasn't verified, plainly. Commit and deploy only when asked.
4. Before a web deploy, ask about the iOS below 18.2 build (see `backend/CLAUDE.md`).
