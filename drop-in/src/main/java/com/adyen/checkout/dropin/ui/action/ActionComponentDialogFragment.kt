/*
 * Copyright (c) 2020 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 4/9/2020.
 */

package com.adyen.checkout.dropin.ui.action

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.adyen.checkout.await.AwaitComponent
import com.adyen.checkout.components.ActionComponent
import com.adyen.checkout.components.ActionComponentData
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.ComponentView
import com.adyen.checkout.components.ViewableComponent
import com.adyen.checkout.components.base.OutputData
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.components.util.ActionTypes
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.exception.ComponentException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.dropin.R
import com.adyen.checkout.dropin.databinding.FragmentActionComponentBinding
import com.adyen.checkout.dropin.getViewFor
import com.adyen.checkout.dropin.ui.base.DropInBottomSheetDialogFragment

@SuppressWarnings("TooManyFunctions")
class ActionComponentDialogFragment : DropInBottomSheetDialogFragment(), Observer<ActionComponentData> {

    companion object {
        private val TAG = LogUtil.getTag()

        const val ACTION = "ACTION"

        fun newInstance(action: Action): ActionComponentDialogFragment {
            val args = Bundle()
            args.putParcelable(ACTION, action)

            val componentDialogFragment = ActionComponentDialogFragment()
            componentDialogFragment.arguments = args

            return componentDialogFragment
        }
    }

    private lateinit var binding: FragmentActionComponentBinding
    private lateinit var action: Action
    private lateinit var actionType: String
    private lateinit var componentView: ComponentView<in OutputData, ViewableComponent<*, *, ActionComponentData>>
    private lateinit var actionComponent: ViewableComponent<*, *, ActionComponentData>
    private var isHandled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d(TAG, "onCreate")
        action = arguments?.getParcelable(ACTION) ?: throw IllegalArgumentException("Action not found")
        actionType = action.type ?: throw IllegalArgumentException("Action type not found")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentActionComponentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.d(TAG, "onViewCreated")
        binding.header.visibility = View.GONE

        try {
            @Suppress("UNCHECKED_CAST")
            componentView = getViewFor(requireContext(), actionType) as ComponentView<in OutputData, ViewableComponent<*, *, ActionComponentData>>
            actionComponent = getComponent(actionType)
            attachComponent(actionComponent, componentView)

            if (!isHandled) {
                (actionComponent as ActionComponent<*>).handleAction(requireActivity(), action)
                isHandled = true
            } else {
                Logger.d(TAG, "action already handled")
            }
        } catch (e: CheckoutException) {
            handleError(ComponentError(e))
        }
    }

    override fun onBackPressed(): Boolean {
        // polling will be canceled by lifecycle event
        protocol.showPaymentMethodsDialog()
        return true
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Logger.d(TAG, "onCancel")
        protocol.terminateDropIn()
    }

    override fun onChanged(actionComponentData: ActionComponentData?) {
        Logger.d(TAG, "onChanged")
        if (actionComponentData != null) {
            protocol.requestDetailsCall(actionComponentData)
        }
    }

    fun setToHandleWhenStarting() {
        Logger.d(TAG, "setToHandleWhenStarting")
        isHandled = false
    }

    /**
     * Return the possible viewable action components
     */
    private fun getComponent(actionType: String): ViewableComponent<*, *, ActionComponentData> {
        return when (actionType) {
            ActionTypes.AWAIT -> {
                AwaitComponent.PROVIDER.get(
                    this,
                    requireActivity().application,
                    dropInViewModel.dropInConfiguration.getConfigurationFor(ActionTypes.AWAIT, requireContext())
                )
            }
            else -> {
                throw ComponentException("Unexpected Action component type - $actionType")
            }
        }
    }

    private fun attachComponent(
        component: ViewableComponent<*, *, ActionComponentData>,
        componentView: ComponentView<in OutputData, ViewableComponent<*, *, ActionComponentData>>
    ) {
        component.observe(this, this)
        component.observeErrors(this, createErrorHandlerObserver())
        binding.componentContainer.addView(componentView as View)
        @Suppress("UNCHECKED_CAST")
        componentView.attach(component, this)
    }

    private fun createErrorHandlerObserver(): Observer<ComponentError> {
        return Observer {
            if (it != null) {
                handleError(it)
            }
        }
    }

    private fun handleError(componentError: ComponentError) {
        Logger.e(TAG, componentError.errorMessage)
        protocol.showError(getString(R.string.action_failed), componentError.errorMessage, true)
    }
}
