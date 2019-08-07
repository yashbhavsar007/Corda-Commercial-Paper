package com.template.contracts

import com.template.states.CommercialState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import com.template.Commands
import net.corda.finance.contracts.utils.sumCashBy
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TransactionBuilder
import net.corda.finance.contracts.CP_PROGRAM_ID
import java.time.Instant
import java.util.*
import net.corda.finance.workflows.asset.CashUtils

// ************
// * Contract *
// ************

class CommercialContract : Contract {
    companion object {
        const val CP_PROGRAM_ID: ContractClassName = "net.corda.finance.contracts.CommercialPaper"
    }

    override fun verify(tx: LedgerTransaction) {

        val groups = tx.groupStates(CommercialState::withoutOwner)

        val command = tx.commands.requireSingleCommand<Commands>()

        val timeWindow: TimeWindow? = tx.timeWindow

        for((inputs,outputs,_) in groups){
            when (command.value){

                is Commands.Move ->{
                    val input = inputs.single()
                    requireThat {
                        " The Transaction is must be signed my CP Owner " using (input.owner.owningKey in command.signers)
                        " The state propagated" using (outputs.size == 1)
                    }
                }

                is Commands.Redeem -> {

                    val input = inputs.single()
                    val received = tx.outputs.map{ it.data }.sumCashBy(input.owner)
                    val time = timeWindow?.fromTime ?: throw IllegalArgumentException("Reedemption must be time stamped")
                    requireThat {
                        " Paper must be matured " using (time >= input.maturityDate)
                        " The received amount must be equal to face value " using (received == input.faceValue)
                        " The paper must be destroyed " using outputs.isEmpty()
                        " Transaction must be signed by owner of CP " using (input.owner.owningKey in command.signers)

                    }
                }

                is Commands.Issue -> {
                    val output = outputs.single()
                    val time = timeWindow?.fromTime ?: throw java.lang.IllegalArgumentException("Issuence must be timestamped")
                    requireThat {
                        " Output states are must be signed by issuing party " using (output.issuance.party.owningKey in command.signers)
                        " Face value must be non negative or non-zero " using (output.faceValue.quantity > 0)
                        " The maturity date must be not in past " using (time < output.maturityDate)
                        " Can't reissue an existing state " using outputs.isEmpty()
                    }
                }

                else -> throw java.lang.IllegalArgumentException("Undefined command")

            }

        }


    }


    fun generateIssue(issuance: PartyAndReference, faceValue: Amount<Issued<Currency>>, maturityDate: Instant,
                      notary: Party): TransactionBuilder {
        val state = CommercialState(issuance , issuance.party , faceValue , maturityDate)
        val stateAndContract = StateAndContract(state, CP_PROGRAM_ID)
        return TransactionBuilder(notary = notary).withItems(stateAndContract, Command(Commands.Issue() , issuance.party.owningKey))
    }

    fun generateMove(tx: TransactionBuilder , paper: StateAndRef<CommercialState> , newOwner: AbstractParty){
        tx.addInputState(paper)
        val outputState = paper.state.data.withNewOwner(newOwner).ownableState
        tx.addOutputState(outputState, CP_PROGRAM_ID)
        tx.addCommand(Command(Commands.Move(), paper.state.data.owner.owningKey ))
    }

    @Throws(InsufficientBalanceException :: class)
    fun generateRedeem(tx: TransactionBuilder , paper: StateAndRef<CommercialState>, services: ServiceHub){
        CashUtils.generateSpend(
                services = services,
                tx = tx,
                amount = paper.state.data.faceValue.withoutIssuer(),
                ourIdentity = services.myInfo.legalIdentitiesAndCerts.single(),
                to = paper.state.data.owner
        )
        tx.addInputState(paper)
        tx.addCommand(Command(Commands.Redeem(), paper.state.data.owner.owningKey))
    }


}