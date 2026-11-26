----------------------------- MODULE Consumption -----------------------------
(***************************************************************************)
(* The coffee count of one user as CoffeeConsumptionServiceImpl implements *)
(* it: adding a coffee (FR-13), undoing it (FR-3), and the correction of   *)
(* an admin (FR-8). The model keeps the stored time of each coffee as      *)
(* "recent" (within its grace period) or "old". Grace periods end in the   *)
(* order in which the user added the coffees. The note of a correction     *)
(* falls into one of the classes that CountCorrection accepts or refuses.  *)
(* MaxCount stands for the largest count that the app stores.              *)
(***************************************************************************)
EXTENDS Integers, Sequences

CONSTANT MaxCount

VARIABLES count, times

vars == <<count, times>>

Recent(time) == time = "recent"
Last(seq) == seq[Len(seq)]
DropLast(seq, n) == IF n >= Len(seq) THEN <<>> ELSE SubSeq(seq, 1, Len(seq) - n)

\* The checks of CoffeeConsumptionServiceImpl.undo
CanUndo(c, t) == c > 0 /\ t # <<>> /\ Recent(Last(t))

TimesAfterAdd(t) == SelectSeq(t, Recent) \o <<"recent">>

TimesAfterCorrection(c, t, newCount) == IF newCount < c THEN DropLast(t, c - newCount) ELSE t

ValidNotes == {"short", "longest"}

InvalidNotes == {"blank", "too long", "NUL character"}

Init == count = 0 /\ times = <<>>

AddCoffee ==
    /\ count < MaxCount
    /\ count' = count + 1
    /\ times' = TimesAfterAdd(times)

RefuseCoffee == count = MaxCount /\ UNCHANGED vars

\* CoffeeConsumptionServiceImpl.applyDelta accepts only a delta of 1 (FR-13).
RefuseDelta == \E delta \in {-1, 0, 2} : UNCHANGED vars

Undo ==
    /\ CanUndo(count, times)
    /\ count' = count - 1
    /\ times' = DropLast(times, 1)

Correct ==
    \E newCount \in 0..MaxCount, note \in ValidNotes :
        /\ count' = newCount
        /\ times' = TimesAfterCorrection(count, times, newCount)

RefuseCorrection ==
    \E newCount \in -1..MaxCount, note \in ValidNotes \cup InvalidNotes :
        /\ newCount < 0 \/ note \in InvalidNotes
        /\ UNCHANGED vars

\* Only an admin corrects a coffee count (FR-8).
RefuseCorrectionByUser == UNCHANGED vars

GracePeriodEnds ==
    \E i \in 1..Len(times) :
        /\ Recent(times[i])
        /\ \A j \in 1..(i - 1) : ~Recent(times[j])
        /\ times' = [times EXCEPT ![i] = "old"]
        /\ UNCHANGED count

Next ==
    \/ AddCoffee \/ RefuseCoffee \/ RefuseDelta \/ Undo
    \/ Correct \/ RefuseCorrection \/ RefuseCorrectionByUser \/ GracePeriodEnds

Spec == Init /\ [][Next]_vars

CountNeverNegative == count >= 0

NoMoreTimesThanCoffees == Len(times) <= count
==============================================================================
