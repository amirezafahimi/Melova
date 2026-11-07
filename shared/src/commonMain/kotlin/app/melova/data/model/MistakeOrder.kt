package app.melova.data.model

import app.melova.audio.scoring.VocalScorer
import kotlin.math.abs

/** How many mistakes of each kind a result shows before the student asks for the rest. */
const val MISTAKES_SHOWN_FIRST = 2

/** What the scorer writes for a note that wasn't sung at all. */
private const val NOT_SUNG = "—"

/** A note left out is as bad as one sung a whole tone off. */
private const val NOT_SUNG_CENTS = 200f

/** How far off a note mistake is, in cents. */
val VocalScorer.PitchCorrection.severity: Float
    get() = if (sungNote == NOT_SUNG) NOT_SUNG_CENTS else abs(centsOff)

val VocalScorer.PitchCorrection.isNotSung: Boolean get() = sungNote == NOT_SUNG

/** The note mistakes, worst first; equal ones keep the order they were sung in. */
fun List<VocalScorer.PitchCorrection>.worstFirst(): List<VocalScorer.PitchCorrection> =
    sortedByDescending { it.severity }

/** Tahrirs and vibratos, the furthest from the teacher's first. */
fun List<TahrirComparison>.furthestFirst(): List<TahrirComparison> = sortedBy { it.score }
