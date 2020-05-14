package piuk.blockchain.android.cards

import android.os.Bundle
import android.view.View
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.remove_card_bottom_sheet.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class RemoveCardBottomSheet : SlidingModalBottomDialog() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onCardRemoved(cardId: String)
    }

    private val custodialWalletManager: CustodialWalletManager by inject()

    private val card: PaymentMethod.Card by unsafeLazy {
        arguments?.getSerializable(CARD_KEY) as? PaymentMethod.Card
            ?: throw IllegalStateException("No card provided")
    }

    private val compositeDisposable = CompositeDisposable()

    override val layoutResource: Int = R.layout.remove_card_bottom_sheet

    override fun initControls(view: View) {
        with(view) {
            title.text = card.uiLabel()
            end_digits.text = card.dottedEndDigits()
            icon.setImageResource(card.cardType.icon())
            rmv_card_btn.setOnClickListener {
                compositeDisposable += custodialWalletManager.deleteCard(card.id)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        updateUi(true)
                    }
                    .doFinally {
                        updateUi(false)
                    }
                    .subscribeBy(onComplete = {
                        (parentFragment as? Host)?.onCardRemoved(card.cardId)
                        dismiss()
                    }, onError = {})
            }
        }
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    private fun updateUi(isLoading: Boolean) {
        view?.progress.visibleIf { isLoading }
        view?.icon.visibleIf { !isLoading }
        view?.rmv_card_btn?.isEnabled = !isLoading
    }

    companion object {
        private const val CARD_KEY = "CARD_KEY"

        fun newInstance(card: PaymentMethod.Card) =
            RemoveCardBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(CARD_KEY, card)
                }
            }
    }
}