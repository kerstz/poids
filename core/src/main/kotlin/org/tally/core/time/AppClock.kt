// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.core.time

/** Source de temps injectable (tests déterministes). Instants en epoch ms UTC. */
interface AppClock {
    fun nowMillis(): Long
}

object SystemAppClock : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
