package com.iamconanpeter.kidsblockbuddy.domain

class UndoStack(private val capacity: Int = 20) {
    private val history = ArrayDeque<WorldGrid>()

    fun push(state: WorldGrid) {
        if (history.size >= capacity) {
            history.removeFirst()
        }
        history.addLast(state)
    }

    fun popOrNull(): WorldGrid? =
        if (history.isEmpty()) null else history.removeLast()

    fun size(): Int = history.size

    fun clear() = history.clear()
}
