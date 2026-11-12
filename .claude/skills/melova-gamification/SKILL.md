---
name: melova-gamification
description: Design and build gamification in the Melova app (shared/ client and its backend) — practice rhythm, personal bests, section mastery, ornament skills, weekly goals, teacher stamps. Use whenever the user asks to make the app more engaging, motivating or "addictive", to add streaks, badges, levels, XP, goals, rewards, celebrations, progress maps or leaderboards, or asks how to bring students back to practise.
---

# Melova gamification

Melova teaches Persian vocal singing (آواز). A student buys a lesson, sings each section, gets a
score and the mistakes (pitch, «تحریر», «ویبراتو», «گذر»), and talks to the teacher in «گفتگو».
Gamification here has one job: **more good practice, and a student who can feel their voice
getting better.** Anything that raises opens or taps without more singing is noise.

Most students live outside Iran (EU consumer and privacy law applies) and are adults paying for lessons. Design for a
serious adult hobby (think a music teacher's notebook with gold stars), not a children's game.

## Ground rules (read before designing anything)

1. **The score is noisy.** The scorer can move several points on changes nobody can hear. So:
   - Never reward a raw score jump, and never punish a drop. Reward *practice* (takes sung,
     days practised) and *personal bests* above a margin (`best + 3` or more).
   - Mastery levels use the section's best score, with wide bands, so one lucky take can't
     flip a level back and forth.
   - Thresholds live in one place (one Kotlin object, or the backend if the server decides),
     tuned against the calibration takes, not scattered as literals.
2. **No leaderboards between students.** The scores are not comparable across voices and
   lessons, and ranking beginners against good singers makes them quit. Compare a student only
   with their own earlier self. A teacher may see their own students' practice, privately.
3. **No dark patterns.** No streak that resets to zero after one missed day, no "you'll lose
   everything" pushes, no paid streak repairs, no fake scarcity. Missing a day costs nothing;
   coming back is greeted, not scolded. At most one reminder push a day, only if the student
   turned it on, and it says something useful («تمرین این هفته: ۲ از ۴ روز»).
4. **The teacher is the strongest reward.** A short word from the teacher beats any badge.
   Prefer mechanics that put the teacher in the loop (stamps, "sent to teacher", replies).
5. **Server is the source of truth.** Progress is computed from `practice_takes` in
   `melova_scoring/student_store.py`, never counted only on the device: a student uses
   Android and the web, and both must show the same numbers.
6. **Celebrate rarely.** A celebration is earned by a new best, a level up, or a goal reached,
   not by every take. One short animation, then the next step («بعدی») — never a modal the
   student must close before singing again.

## Mechanics that fit Melova (in order of value for effort)

Each lists the data it needs. Check what already exists before adding anything:
`practice_progress()` returns each section's best and the latest take, and
`record_practice_take()` already returns `is_new_best`.

1. **«رکورد تازه» — personal best moment** (data: exists). After a take that beats the section
   best by the margin, show the old and new best side by side with a soft accent chip and a
   button to listen to both. Cheapest, most honest win.
2. **Section mastery** (data: exists). Each section gets a level from its best score, shown as
   the status at the end of its row (the progress ring already there, filled in steps):
   «شروع» → «آشنا» → «مسلط». A lesson's ring fills with its sections' levels. Don't add a
   third line to a row (see STANDARDS: row anatomy).
3. **Weekly practice rhythm, not a daily streak** (data: `created_at` on takes; add an endpoint
   or extend `practice_progress`). Show «روزهای تمرین این هفته» as seven small dots on home. The
   student picks a weekly goal (2–6 days, default 3); a day counts once at least one scored
   take was sung that day *in the student's own time zone* (send the offset from the client).
   Reaching the goal fills the week; weeks reached in a row is the only "streak", and a missed
   week only pauses it.
4. **Ornament skills** (data: mistakes in `result_json`). Three tracks, «تحریر», «ویبراتو»,
   «گذر», each counting ornaments sung without a mistake across all lessons. Shown on the
   profile as three small bars. Ties the game to what the teacher actually teaches.
5. **Teacher stamp «آفرین»** (data: new — a stamp on a practice message in chat). The teacher can
   stamp a take the student sent; it shows on that section's row and in the student's list of
   stamped takes. Needs a column on the chat message, added in `_SCHEMA` only.
6. **Practice journal** (data: exists). A page listing the best take of each section over time,
   with a play button, so the student can hear week 1 against week 6. This is the most
   convincing proof of progress a singer can get.
7. **Lesson path** (data: exists). The lesson page as a path of sections with the next one
   marked `highlighted` with a «بعدی» chip; finishing a lesson (every section at «آشنا» or
   above) suggests the next lesson of the same teacher.

Avoid unless the user insists, and say why: XP/coins currencies, leagues, public profiles
with scores, loot or random rewards, purchased boosts.

## How to build one

1. **Pick one mechanic** and say in one line what it rewards and why it can't be gamed by the
   scorer's noise. Ask the user only if the choice changes the product (e.g. a weekly goal UI).
2. **Backend first** (`backend/`): logic in `melova_scoring/` (usually `student_store.py`),
   route in `api/student.py` behind `require_student`. Compute from `practice_takes`; a new
   column goes in `_SCHEMA` only, nullable or with a default. Add a test in `tests/` named as a
   sentence («a missed week pauses the weekly run but doesn't reset it»).
3. **Client** (`shared/src/commonMain/kotlin/app/melova/`): DTO in `data/remote/dto`, mapper,
   model in `data/model`, a call through `apiCall` in the repository, values in the view
   model's `UiState`. Screens get values and callbacks only.
4. **Page**: reuse `MelovaSurfaceCard`, `SectionHeader`, `MelovaProgressBar`, the existing
   progress ring; colours from `ui/theme` (a win uses `MelovaAccentTint` with
   `MelovaAccentSoft`, never a new literal colour); sizes from `MelovaDimens`; digits through
   the Persian font; all words Persian, no English, no «سرور». Keep the page's one main action
   (record) as the only solid fill — a celebration is a tint, not a second big button.
5. **Animation**: short (200–400 ms), `animateFloatAsState`/`AnimatedVisibility`, nothing that
   blocks recording again. Check it on the web build too (wasm is slower).
6. **Check** against `STANDARDS.md`, compile all three targets
   (`./gradlew :shared:compileDebugKotlinAndroid :shared:compileKotlinJs :shared:compileKotlinWasmJs`),
   run the backend tests, and open the page at phone width in both themes, signed in and
   signed out (signed out shows no progress, only «ورود یا ثبت‌نام»).

## Wording

Use these, so pages agree: «رکورد تازه», «بهترین نمره», «روزهای تمرین این هفته», «هدف هفته»,
«هفته‌های پشت سر هم», «شروع» / «آشنا» / «مسلط», «آفرین استاد», «دفتر تمرین», «بعدی».
Encourage, don't judge: «نزدیک شدی، یک بار دیگر» rather than «نمره کم است».
