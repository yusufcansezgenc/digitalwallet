package com.inghubs.digitalwallet.services;

import java.util.UUID;

import com.inghubs.digitalwallet.dtos.requests.*;
import com.inghubs.digitalwallet.dtos.responses.*;
import com.inghubs.digitalwallet.utilities.security.CustomUserDetails;

public interface WalletService {
    ListWalletResponse ListWallets(UUID customerId);
    CreateWalletResponse CreateWallet(CreateWalletRequest request, CustomUserDetails userDetails);
    DepositWalletResponse DepositWallet(DepositWalletRequest request);
    WithdrawWalletResponse WithdrawWallet(WithdrawWalletRequest request, CustomUserDetails userDetails);
}
