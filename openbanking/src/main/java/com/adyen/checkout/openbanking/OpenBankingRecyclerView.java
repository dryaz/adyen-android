/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 12/6/2019.
 */

package com.adyen.checkout.openbanking;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adyen.checkout.components.model.payments.request.OpenBankingPaymentMethod;
import com.adyen.checkout.issuerlist.IssuerListRecyclerView;

public class OpenBankingRecyclerView extends IssuerListRecyclerView<OpenBankingPaymentMethod, OpenBankingComponent> {

    public OpenBankingRecyclerView(@NonNull Context context) {
        super(context);
    }

    public OpenBankingRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OpenBankingRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
