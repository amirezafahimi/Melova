# Melova standards

Check every change against this list before calling it done. If a change needs to break a
rule, say so and update this file in the same change.

## Language and wording

- Student and teacher pages are Persian. Never «سرور», a model name, an error code or a raw
  exception: say «توسط برنامه» or nothing. English labels already on a page (such as `T`, `V`,
  `G`, `s` and the audio format line) stay as they are unless the user asks to change them.
- The same goes for text the backend sends (`detail=`, failure reasons).
- «جستجو», not «کاوش». «گفتگو» for chats. «تحریر», «ویبراتو», «گذر» for the three ornaments.
- No guest concept on any page. Someone who hasn't signed in sees «ورود یا ثبت‌نام», and never
  sees followed teachers.
- Code comments and identifiers are English. Persian in a comment only quotes page text («…»).
- Comments say why, in plain sentences. Match the amount of comment around them.

## Pages (Compose)

- Colours come from `ui/theme`. A literal `Color(0x…)` is only for a third party's brand (Google
  button) and the lesson cover gradients.
- Solid accent fill takes white content; `MelovaAccentTint` takes `MelovaAccentSoft`.
- Reuse before writing: `MelovaSurfaceCard`, `MelovaTopBar`, `MelovaNavRow` for a row that opens
  something, `SectionHeader`, `ConfirmDialog`, `MelovaBackground`, `RtlContent`.
- A new page is `MelovaBackground { RtlContent { LazyColumn … } }` with system bar insets and
  20.dp sides, and gets a route in `AppScreen` and a `composable` in `MelovaApp`.
- Works at phone width in both themes. Anything shown over a photo or accent stays readable.
- Numbers shown to people use Persian digits (`formatPersianInteger`).

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
- A new endpoint checks who is asking (`require_student`, `_who`, `_require_lesson_access`).
- Secrets live in `deploy/melova-api.env`, never in the example file, code or logs. Never turn on
  `MELOVA_LOGIN_LOG_CODES` on the server.
- New behaviour gets a test in `tests/`. Tests name the behaviour, as a sentence.
- A new dependency goes in `requirements.txt`; `update_server.sh` installs it on deploy.

## Before saying done

1. Run it: backend tests, and the changed page in the browser, at phone width.
2. Check nothing else moved: the path before and after your change, signed in and signed out.
3. Report what was and wasn't verified, plainly. Commit and deploy only when asked.
4. Before a web deploy, ask about the iOS below 18.2 build (see `backend/CLAUDE.md`).
