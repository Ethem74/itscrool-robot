package io.adev.itschool.robot.common.arena

import io.adev.itschool.robot.common.arena.entity.Position
import io.adev.itschool.robot.common.arena.entity.RobotState
import io.adev.itschool.robot.common.arena.entity.arena.Arena
import io.adev.itschool.robot.common.arena.entity.arena.RobotStateMutationsProvider

class Robot(
    private val initialState: RobotState,
    private val stateMutationsProvider: RobotStateMutationsProvider,
    private val applyStates: (List<RobotState>) -> Unit,
) : RobotState.Source {

    fun requireWon() {
        if (!currentState.isWon) {
            throw NotCompleteException(currentState, stateHistory)
        }
    }

    private lateinit var currentState: RobotState

    fun display(text: String) {
        updateState(state = currentState.display(text))
    }

    fun right(stepsCount: Int = 1) {
        repeat(stepsCount) {
            move(movement = Position.Movement.Right)
        }
    }

    fun left(stepsCount: Int = 1) {
        repeat(stepsCount) {
            move(movement = Position.Movement.Left)
        }
    }

    fun down(stepsCount: Int = 1) {
        repeat(stepsCount) {
            move(movement = Position.Movement.Down)
        }
    }

    fun up(stepsCount: Int = 1) {
        repeat(stepsCount) {
            move(movement = Position.Movement.Up)
        }
    }

    fun move(movement: Position.Movement) {
        updateState(state = currentState.move(movement, source = this))
    }

    fun applyInitialState() {
        updateState(state = initialState)
    }

    private fun updateState(state: RobotState) {
        val statesList = makeStatesList(state)
        applyStates(statesList)
    }

    private fun makeStatesList(state: RobotState): List<RobotState> {

        val list = mutableListOf<RobotState>()

        val beforeState = stateMutationsProvider.beforeRobotMove(robotState = state).takeIf { it != state }
        if (beforeState != null) {
            val beforeStates = makeStatesList(beforeState)
            list.addAll(beforeStates)
        }

        list.add(state)

        val afterState = stateMutationsProvider.afterRobotMove(robotState = state).takeIf { it != state }
        if (afterState != null) {
            val afterStates = makeStatesList(afterState)
            list.addAll(afterStates)
        }

        return list
    }

    private val stateHistory = mutableListOf<RobotState>()
    fun onStateApplied(state: RobotState) {
        stateHistory.add(state)
        currentState = state
        if (state.isDestroyed) {
            throw RobotDestroyedException(state, stateHistory)
        }
    }

    override fun sourceRepresentation(): String {
        return this::class.simpleName.toString()
    }
}

interface RobotExecutor {

    fun execute(
        robot: Robot, arena: Arena, userAction: UserAction,
        callback: Callback, useCallback: (() -> Unit) -> Unit,
    )

    interface Callback {
        fun onWon()
        fun onFailure(e: Exception)
    }
}

typealias UserAction = (Robot, Arena) -> Unit

interface RobotStatesApplier {

    fun applyStates(
        states: List<RobotState>,
        callback: Callback, useCallback: (() -> Unit) -> Unit,
    )

    fun robotMoved()

    interface Callback {
        fun moveRobot(state: RobotState)
        fun onStateApplied(state: RobotState)
    }
}

class RobotDestroyedException(
    state: RobotState,
    stateHistory: List<RobotState>,
) : IllegalStateException(
    "robot is destroyed, state=$state, history:\n${formatHistory(stateHistory)}"
)

class NotCompleteException(
    state: RobotState,
    stateHistory: List<RobotState>,
) : IllegalStateException(
    "level is not completed, state=$state, history:\n${formatHistory(stateHistory)}"
)

private fun formatHistory(stateHistory: List<RobotState>): String {
    return stateHistory.joinToString(separator = " ->\n") { "(${it})" }
}