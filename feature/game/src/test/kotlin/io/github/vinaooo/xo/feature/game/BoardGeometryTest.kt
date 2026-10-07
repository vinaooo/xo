package io.github.vinaooo.xo.feature.game

import io.github.vinaooo.xo.feature.game.board.BoardGeometry
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BoardGeometryTest {
    @Test
    fun `a tap falls in the cell under it, row by row`() {
        val geometry = BoardGeometry(side = 3, sizePx = 300f)
        geometry.cellAt(10f, 10f) shouldBe 0
        geometry.cellAt(299f, 10f) shouldBe 2
        geometry.cellAt(150f, 150f) shouldBe 4
        geometry.cellAt(100f, 200f) shouldBe 7
        geometry.cellAt(299.9f, 299.9f) shouldBe 8
    }

    @Test
    fun `outside the board is no cell`() {
        val geometry = BoardGeometry(side = 5, sizePx = 500f)
        geometry.cellAt(-1f, 10f).shouldBeNull()
        geometry.cellAt(10f, 500f).shouldBeNull()
        geometry.cellAt(500f, 10f).shouldBeNull()
    }

    @Test
    fun `cells are centered in their squares`() {
        val geometry = BoardGeometry(side = 4, sizePx = 400f)
        geometry.cellPx shouldBe 100f
        geometry.center(0) shouldBe (50f to 50f)
        geometry.center(6) shouldBe (250f to 150f)
    }
}
