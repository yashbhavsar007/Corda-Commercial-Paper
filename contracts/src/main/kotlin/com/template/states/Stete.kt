package com.template.states

import com.template.contracts.CommercialContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PartyAndReference
import net.corda.core.identity.AbstractParty
import net.corda.core.contracts.Issued
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AnonymousParty
import java.time.Instant
import net.corda.core.identity.Party
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(CommercialContract::class)

data class CommercialState(
                            val issuance : PartyAndReference,
                            override val owner : AbstractParty,
                            val faceValue : Amount<Issued<Currency>>,
                            val maturityDate : Instant): ContractState{

    override val participants = listOf(owner)

    fun withoutOwner() = copy(owner = AnonymousParty(NullKeys.NullPublicKey))

    override fun withNewOwner(newOwner : AbstractParty) = 
}