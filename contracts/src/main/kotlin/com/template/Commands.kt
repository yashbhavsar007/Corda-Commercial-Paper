package com.template

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.TypeOnlyCommandData

interface Commands : CommandData {
    class Move : TypeOnlyCommandData(), Commands
    class Redeem : TypeOnlyCommandData(), Commands
    class Issue : TypeOnlyCommandData(), Commands
}
