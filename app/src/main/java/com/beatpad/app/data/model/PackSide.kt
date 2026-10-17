package com.beatpad.app.data.model

enum class PackSide {
    SIDE_A,
    SIDE_B;

    fun toggle(): PackSide = if (this == SIDE_A) SIDE_B else SIDE_A
}
