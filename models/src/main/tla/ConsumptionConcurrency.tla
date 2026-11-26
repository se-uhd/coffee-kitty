------------------------ MODULE ConsumptionConcurrency ------------------------
(***************************************************************************)
(* Requests that add a coffee, undo a coffee, or correct the coffee count  *)
(* of the same user at the same time. Each request reads the coffee count  *)
(* and the coffee times, computes its result with the operators of         *)
(* Consumption, and stores the result in one transaction, unless           *)
(* CoffeeConsumptionServiceImpl refuses the request. With UseLock, the     *)
(* read takes the row lock of getByUserIdForUpdate, which the transaction  *)
(* holds until it ends. Time does not pass while the requests run.         *)
(***************************************************************************)
EXTENDS Consumption, FiniteSets

CONSTANTS Requests, UseLock

VARIABLES start, kind, target, pc, seen, lock

allVars == <<count, times, start, kind, target, pc, seen, lock>>

Kinds == {"add", "undo", "correct"}

Starts == {<<0, <<>>>>, <<1, <<"recent">>>>, <<1, <<"old">>>>, <<MaxCount, <<"old", "recent">>>>}

Refused(k, s) == (k = "add" /\ s[1] = MaxCount) \/ (k = "undo" /\ ~CanUndo(s[1], s[2]))

Effect(k, newCount, s) ==
    IF Refused(k, s) THEN s
    ELSE CASE k = "add" -> <<s[1] + 1, TimesAfterAdd(s[2])>>
           [] k = "undo" -> <<s[1] - 1, DropLast(s[2], 1)>>
           [] k = "correct" -> <<newCount, TimesAfterCorrection(s[1], s[2], newCount)>>

RECURSIVE Serial(_, _)
Serial(order, s) ==
    IF order = <<>> THEN s
    ELSE Serial(Tail(order), Effect(kind[Head(order)], target[Head(order)], s))

Orders == {o \in [1..Cardinality(Requests) -> Requests] : \A i, j \in DOMAIN o : i # j => o[i] # o[j]}

AllDone == \A r \in Requests : pc[r] = "done"

ConcurrentInit ==
    /\ start \in Starts
    /\ count = start[1]
    /\ times = start[2]
    /\ kind \in [Requests -> Kinds]
    /\ target \in [Requests -> 0..MaxCount]
    /\ pc = [r \in Requests |-> "start"]
    /\ seen = [r \in Requests |-> start]
    /\ lock = {}

ReadState(r) ==
    /\ pc[r] = "start"
    /\ UseLock => lock = {}
    /\ lock' = IF UseLock THEN {r} ELSE lock
    /\ seen' = [seen EXCEPT ![r] = <<count, times>>]
    /\ pc' = [pc EXCEPT ![r] = "read"]
    /\ UNCHANGED <<count, times, start, kind, target>>

StoreResult(r) ==
    /\ pc[r] = "read"
    /\ IF Refused(kind[r], seen[r])
          THEN UNCHANGED <<count, times>>
          ELSE LET result == Effect(kind[r], target[r], seen[r])
               IN count' = result[1] /\ times' = result[2]
    /\ pc' = [pc EXCEPT ![r] = "done"]
    /\ lock' = lock \ {r}
    /\ UNCHANGED <<start, kind, target, seen>>

Finished == AllDone /\ UNCHANGED allVars

ConcurrentNext == (\E r \in Requests : ReadState(r) \/ StoreResult(r)) \/ Finished

ConcurrentSpec == ConcurrentInit /\ [][ConcurrentNext]_allVars

Serializable == AllDone => \E o \in Orders : <<count, times>> = Serial(o, start)
==============================================================================
