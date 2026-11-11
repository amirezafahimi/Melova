# Melova product direction

Decisions from the product conversation, for the next implementation. Numbers marked as examples are not locked.

Melova is a vocal practice app. The student sings a section against a reference performance and sees the gap: pitch, tahrir, vibrato, glide. The teacher is the author of that performance. The scorer does the daily correction. The teacher comes in when a take is sent to them.

## Keeping the student

The student comes back to one unfinished, specific task. A lesson list does not do that, and neither does a day streak.

The card at the top of the practice tab («امروز چه چیزی تمرین کنیم؟») states one sentence from the last take and opens that same section in one tap. The sentence is built from data the score already has: the weak point, the tahrir comparison, the last score, the best score, and whether this take set a new best.

Tone examples, not final copy:

- «تحریر دوم کوتاه بود. همان را دوباره.»
- «بهترینت ۸۴ بود، آخرین اجرا ۷۱. همان بخش.»
- «از ۶۸ رسیدی به ۸۱. بخش بعد باز است.»

Solfege is for a day without time for a full section: a short warm-up, with no teacher. «استادت به آخرین اجرا گوش داد» is a reason to come back, because chat already exists.

A day-streak counter is out. In singing, a broken chain pushes people away. The gap from their own previous take brings them back.

## Lesson-first, not teacher-first

The unit of the product is the lesson. The student builds their own shelf from any teacher. The explore tab is already a list of lessons, and it stays that way. A teacher page is a filter: see this person's lessons. The front door and the «درس‌های من» shelf are the lesson.

The teacher's name stays on the lesson card. Ten people teach the same piece, and the student has to know whose voice they are matching.

A catalog with no order is a pile of unrelated recordings, and the styles fight each other. Each lesson has a level, and the sensible next section is clear. Choice stays open. The path is visible.

Teacher-first is the right shape when the person is the product: live weekly correction and assignments. Here that role belongs to the scorer.

## Money

The shelf will be full of lessons. What is sold is access to that shelf: one subscription, every lesson, from any teacher. The lesson stays the unit of daily work. The subscription is only the checkout.

A separate payment per lesson is out. So is card-to-card onto each teacher's card: that ties the choice of a lesson to the choice of a person. Payment happens inside Melova, and the teacher is paid from that sale.

The monthly allowance is stated in lessons, not coins. Each month the student unlocks a few works. When works differ a lot in length, the weight is shown: a short work is one share, a full course is three (example). The month's total, for example four, is written on the subscription itself. A balance of «100 coins» and coin prices on works are out. A coin count does not mean toman, it fakes a sense of plenty, and the next step is selling extra coin packs.

Unlocking a work spends a share. Practice after unlock is unlimited. Spending a share on every take closes the loop that should bring them back each day.

A teacher is paid when their work is unlocked, in proportion to its weight. A work nobody unlocks earns nothing.

An unused share lasts one more month only. Longer than that, it piles up. Dropping it to zero on the last night pushes a random unlock.

The first section of every work is free, so the voice and the score are visible before subscribing.

## Code versus this decision

Sales are per lesson today. A lesson has `priceToman` and `bazaarSku`, opens when it is free or owned (`Lesson.isOpen`), and a card transfer is confirmed by the teacher (`buyLesson`, purchase requests on the teacher desk). The continue card shows the last lesson, its score, and the next section, not a sentence about what was weak in the last take.

This document replaces that model. Until implementation starts, the code keeps the old behavior.

## Left out

- A day-streak counter.
- An unlimited whole-shelf subscription, until the monthly allowance is stated in lessons.
- A multi-lesson pack for a thin shelf. The shelf is full, and a pack is an artificial limit.
- Coins, a coin exchange rate, and extra coin packs.
- A home or explore tab organized around the teacher.
- Spending a share on each take or section after the work is unlocked.
