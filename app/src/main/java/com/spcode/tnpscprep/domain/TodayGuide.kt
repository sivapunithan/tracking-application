package com.spcode.tnpscprep.domain

/** What the "Next best action" card suggests. Pure rules, no Android. */
sealed interface NextAction {
    data object SetExamDate : NextAction
    data class Revise(val dueCount: Int) : NextAction
    data object StartFocus : NextAction
    data object LogFood : NextAction
    /** The first unfinished item from the plan she made in the evening wrap-up. */
    data class PlannedTask(val taskId: Long, val title: String) : NextAction
    data object NewTopic : NextAction
    data object AllCaughtUp : NextAction
}

data class NextActionInput(
    val hasExamDate: Boolean,
    val dueCount: Int,
    val focusMinutesToday: Int,
    val hour: Int,
    val foodLoggedToday: Boolean,
    val hasNewTopic: Boolean,
    /** First undone planned task for today (from the evening wrap-up), or null. */
    val planned: PlannedTaskRef? = null,
)

data class PlannedTaskRef(val id: Long, val title: String)

object TodayGuide {
    const val EVENING_HOUR = 18

    /**
     * Rule ORDER (first match wins):
     *  1. no exam date            -> set it (everything else is easier with a target)
     *  2. revisions are due       -> revise (the schedule is the app's core; overdue topics fade fastest)
     *  3. no focus session today  -> start one
     *  4. evening, food not logged-> log food (a 5-second habit, only worth asking late in the day)
     *  5. a planned task is open  -> the first one (she chose it herself last night; it never overrides a due revision, a focus session or the food prompt)
     *  6. an unread topic exists  -> start a new topic
     *  7. otherwise               -> all caught up
     */
    fun nextAction(i: NextActionInput): NextAction = when {
        !i.hasExamDate -> NextAction.SetExamDate
        i.dueCount > 0 -> NextAction.Revise(i.dueCount)
        i.focusMinutesToday <= 0 -> NextAction.StartFocus
        i.hour >= EVENING_HOUR && !i.foodLoggedToday -> NextAction.LogFood
        i.planned != null -> NextAction.PlannedTask(i.planned.id, i.planned.title)
        i.hasNewTopic -> NextAction.NewTopic
        else -> NextAction.AllCaughtUp
    }
}

enum class GettingStartedStep { EXAM_DATE, FIRST_FOCUS, FIRST_FOOD, FIRST_DIARY }

data class GettingStartedItem(val step: GettingStartedStep, val done: Boolean)

object GettingStarted {
    fun items(examDateSet: Boolean, anySession: Boolean, anyFood: Boolean, anyDiary: Boolean): List<GettingStartedItem> = listOf(
        GettingStartedItem(GettingStartedStep.EXAM_DATE, examDateSet),
        GettingStartedItem(GettingStartedStep.FIRST_FOCUS, anySession),
        GettingStartedItem(GettingStartedStep.FIRST_FOOD, anyFood),
        GettingStartedItem(GettingStartedStep.FIRST_DIARY, anyDiary),
    )

    fun isComplete(items: List<GettingStartedItem>) = items.all { it.done }
}

/** What a plan line asks her to do. The UI resolves names from the ids. */
enum class PlanKind { REVISION, READ, NEWS }

data class PlanPick(val kind: PlanKind, val refId: String)

/** A due item as the plan builder sees it: the id and whether it is a current-affairs item. */
data class DuePick(val id: String, val isAffair: Boolean)

object PlanBuilder {
    const val MAX = 3
    const val MAX_REVISIONS = 2

    /**
     * Today's plan: at most [MAX] items from EXISTING data, in this priority: due revisions (at most [MAX_REVISIONS], so there is room for something new),
     * the next unread topic, a current-affairs item to review. It is built ONCE per day and then stored (planned_task), so ticking an item
     * never makes a new one appear: the list is finite and the progress is "done of these items".
     */
    fun build(due: List<DuePick>, nextTopicId: String?, affairId: String?): List<PlanPick> {
        val out = ArrayList<PlanPick>()
        due.take(MAX_REVISIONS).forEach { out += PlanPick(if (it.isAffair) PlanKind.NEWS else PlanKind.REVISION, it.id) }
        if (nextTopicId != null) out += PlanPick(PlanKind.READ, nextTopicId)
        if (affairId != null) out += PlanPick(PlanKind.NEWS, affairId)
        // fill with more due revisions if there was nothing new, never more than MAX, never twice the same id
        due.drop(MAX_REVISIONS).forEach { out += PlanPick(if (it.isAffair) PlanKind.NEWS else PlanKind.REVISION, it.id) }
        return out.distinctBy { it.refId }.take(MAX)
    }
}

/** Evening wrap-up card on Today: when to show it and in which state. Pure. */
enum class WrapupCardState { HIDDEN, INVITE, DONE }

object WrapupCard {
    /** [hour] 0..23, [startHour] = the time she set (default 18). Hours before the 3 AM rollover still belong to "this evening". */
    fun state(hour: Int, startHour: Int, wrappedToday: Boolean): WrapupCardState {
        val evening = hour >= startHour || hour < LogicalDay.ROLLOVER_HOUR
        return when {
            !evening -> WrapupCardState.HIDDEN
            wrappedToday -> WrapupCardState.DONE
            else -> WrapupCardState.INVITE
        }
    }
}
