package com.inghubs.digitalwallet.WalletTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.inghubs.digitalwallet.dtos.requests.*;
import com.inghubs.digitalwallet.dtos.responses.*;
import com.inghubs.digitalwallet.entities.*;
import com.inghubs.digitalwallet.repositories.*;
import com.inghubs.digitalwallet.services.*;
import com.inghubs.digitalwallet.utilities.constants.*;
import com.inghubs.digitalwallet.utilities.enums.*;
import com.inghubs.digitalwallet.utilities.exceptions.*;
import com.inghubs.digitalwallet.utilities.security.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
@DisplayName("Wallet Service Tests")
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private WalletService walletService;

    private UUID customerId;
    private UUID walletId;
    private Customer testCustomer;
    private Wallet testWallet;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        walletId = UUID.randomUUID();

        testCustomer = Customer.builder()
                .id(customerId)
                .name("Name")
                .surname("Surname")
                .TCKN("12345678901")
                .build();

        testWallet = Wallet.builder()
                .id(walletId)
                .customer(testCustomer)
                .walletName("Wallet")
                .currency(Currency.TRY)
                .balance(5000.0)
                .usableBalance(5000.0)
                .isActiveShopping(true)
                .isActiveWithdraw(true)
                .build();

        testUserDetails = CustomUserDetails.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .username("testuser")
                .password("password")
                .role(Role.EMPLOYEE)
                .build();
    }

    // ============== ListWallets Tests ==============

    @Test
    @DisplayName("Should return wallet list when customer exists")
    void testListWallets_CustomerExists_ReturnsWalletList() {
        // Arrange
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(testWallet);

        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(walletRepository.findByCustomerId(customerId)).thenReturn(walletList);

        // Act
        ListWalletResponse response = walletService.ListWallets(customerId);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getWallets().size());
        assertEquals(walletId, response.getWallets().get(0).getId());
        verify(customerRepository, times(1)).existsById(customerId);
        verify(walletRepository, times(1)).findByCustomerId(customerId);
    }

    @Test
    @DisplayName("Should throw not found exception when customer does not exist")
    void testListWallets_CustomerNotFound_ThrowsNotFoundException() {
        // Arrange
        when(customerRepository.existsById(customerId)).thenReturn(false);

        // Assert
        assertThrows(NotFoundException.class, () -> walletService.ListWallets(customerId));
        verify(customerRepository, times(1)).existsById(customerId);
        verify(walletRepository, never()).findByCustomerId(any());
    }

    @Test
    @DisplayName("Should return empty list when customer has no wallets")
    void testListWallets_NoWallets_ReturnsEmptyList() {
        // Arrange
        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(walletRepository.findByCustomerId(customerId)).thenReturn(new ArrayList<>());

        // Act
        ListWalletResponse response = walletService.ListWallets(customerId);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getWallets().size());
        verify(walletRepository, times(1)).findByCustomerId(customerId);
    }

    // ============== CreateWallet Tests ==============

    @Test
    @DisplayName("Should create wallet successfully when customer exists")
    void testCreateWallet_CustomerExists_CreatesWallet() {
        // Arrange
        CreateWalletRequest request = CreateWalletRequest.builder()
                .customerId(customerId)
                .walletName("New Wallet")
                .currency(Currency.EUR)
                .isActiveShopping(true)
                .isActiveWithdraw(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        CreateWalletResponse response = walletService.CreateWallet(request, testUserDetails);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getWallet());
        assertEquals(walletId, response.getWallet().getId());
        verify(customerRepository, times(1)).findById(customerId);
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when customer does not exist")
    void testCreateWallet_CustomerNotFound_ReturnsNull() {
        // Arrange
        CreateWalletRequest request = CreateWalletRequest.builder()
                .customerId(customerId)
                .walletName("New Wallet")
                .currency(Currency.EUR)
                .isActiveShopping(true)
                .isActiveWithdraw(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> walletService.CreateWallet(request, testUserDetails));
        verify(customerRepository, times(1)).findById(customerId);
        verify(walletRepository, never()).save(any());
    }

    // ============== DepositWallet Tests ==============

    @Test
    @DisplayName("Should deposit successfully when amount is below limit")
    void testDepositWallet_AmountBelowLimit_DepositsSuccessfully() {
        // Arrange
        double depositAmount = 500.0;
        DepositWalletRequest request = DepositWalletRequest.builder()
                .walletId(walletId)
                .amount(depositAmount)
                .source("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        Transaction expectedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(testWallet)
                .amount(depositAmount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(transactionService.CreateTransaction(any(Transaction.class))).thenReturn(expectedTransaction);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        DepositWalletResponse response = walletService.DepositWallet(request);

        // Assert
        assertNotNull(response);
        assertFalse(response.getIsPendingTransaction());
        assertEquals(TransactionStatus.APPROVED, response.getTransaction().getStatus());
        assertEquals(depositAmount, response.getTransaction().getAmount());
        verify(walletRepository, atLeast(2)).findById(walletId);
        verify(transactionService, times(1)).CreateTransaction(any(Transaction.class));
    }

    @Test
    @DisplayName("Should create pending transaction when amount exceeds limit")
    void testDepositWallet_AmountExceedsLimit_CreatesPendingTransaction() {
        // Arrange
        double depositAmount = WalletConstants.AMOUNT_LIMIT + 500.0;
        DepositWalletRequest request = DepositWalletRequest.builder()
                .walletId(walletId)
                .amount(depositAmount)
                .source("Large Deposit")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        Transaction expectedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(testWallet)
                .amount(depositAmount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .oppositeParty("Large Deposit")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(transactionService.CreateTransaction(any(Transaction.class))).thenReturn(expectedTransaction);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        DepositWalletResponse response = walletService.DepositWallet(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getIsPendingTransaction());
        assertEquals(TransactionStatus.PENDING, response.getTransaction().getStatus());
        verify(transactionService, times(1)).CreateTransaction(any(Transaction.class));
    }

    @Test
    @DisplayName("Should return null when wallet does not exist during deposit")
    void testDepositWallet_WalletNotFound_ReturnsNull() {
        // Arrange
        DepositWalletRequest request = DepositWalletRequest.builder()
                .walletId(walletId)
                .amount(500.0)
                .source("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // Assert
         assertThrows(NotFoundException.class, () -> walletService.DepositWallet(request));
        verify(walletRepository, times(1)).findById(walletId);
        verify(transactionService, never()).CreateTransaction(any());
    }

    // ============== WithdrawWallet Tests ==============

    @Test
    @DisplayName("Should withdraw successfully when amount is below limit and withdrawal is active")
    void testWithdrawWallet_ValidAmount_WithdrawsSuccessfully() {
        // Arrange
        double withdrawAmount = 500.0;
        WithdrawWalletRequest request = WithdrawWalletRequest.builder()
                .walletId(walletId)
                .amount(withdrawAmount)
                .destination("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        Transaction expectedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(testWallet)
                .amount(withdrawAmount)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(transactionService.CreateTransaction(any(Transaction.class))).thenReturn(expectedTransaction);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        WithdrawWalletResponse response = walletService.WithdrawWallet(request, testUserDetails);

        // Assert
        assertNotNull(response);
        assertFalse(response.getIsPendingTransaction());
        assertEquals(TransactionStatus.APPROVED, response.getTransaction().getStatus());
        verify(transactionService, times(1)).CreateTransaction(any(Transaction.class));
    }

    @Test
    @DisplayName("Should create pending withdrawal when amount exceeds limit")
    void testWithdrawWallet_AmountExceedsLimit_CreatesPendingTransaction() {
        // Arrange
        double withdrawAmount = WalletConstants.AMOUNT_LIMIT + 500.0;
        WithdrawWalletRequest request = WithdrawWalletRequest.builder()
                .walletId(walletId)
                .amount(withdrawAmount)
                .destination("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        Transaction expectedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(testWallet)
                .amount(withdrawAmount)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.PENDING)
                .oppositeParty("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(transactionService.CreateTransaction(any(Transaction.class))).thenReturn(expectedTransaction);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        WithdrawWalletResponse response = walletService.WithdrawWallet(request, testUserDetails);

        // Assert
        assertNotNull(response);
        assertTrue(response.getIsPendingTransaction());
        assertEquals(TransactionStatus.PENDING, response.getTransaction().getStatus());
    }

    @Test
    @DisplayName("Should throw exception when withdrawal is not active")
    void testWithdrawWallet_WithdrawalNotActive_ThrowsException() {
        // Arrange
        Wallet inactiveWithdrawWallet = Wallet.builder()
                .id(walletId)
                .customer(testCustomer)
                .walletName("Main Wallet")
                .currency(Currency.TRY)
                .balance(5000.0)
                .usableBalance(5000.0)
                .isActiveShopping(true)
                .isActiveWithdraw(false)
                .build();

        WithdrawWalletRequest request = WithdrawWalletRequest.builder()
                .walletId(walletId)
                .amount(500.0)
                .destination("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(inactiveWithdrawWallet));

        // Act & Assert
        assertThrows(WithdrawalDeniedException.class, () -> walletService.WithdrawWallet(request, testUserDetails));
        verify(transactionService, never()).CreateTransaction(any());
    }

    @Test
    @DisplayName("Should throw exception when shopping payment attempted on inactive wallet")
    void testWithdrawWallet_ShoppingPaymentInactiveWallet_ThrowsException() {
        // Arrange
        Wallet inactiveShoppingWallet = Wallet.builder()
                .id(walletId)
                .customer(testCustomer)
                .walletName("Main Wallet")
                .currency(Currency.TRY)
                .balance(5000.0)
                .usableBalance(5000.0)
                .isActiveShopping(false)
                .isActiveWithdraw(true)
                .build();

        WithdrawWalletRequest request = WithdrawWalletRequest.builder()
                .walletId(walletId)
                .amount(500.0)
                .destination("Merchant ABC")
                .oppositePartyType(OppositePartyType.PAYMENT)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(inactiveShoppingWallet));

        // Act & Assert
        assertThrows(WithdrawalDeniedException.class, () -> walletService.WithdrawWallet(request, testUserDetails));
        verify(transactionService, never()).CreateTransaction(any());
    }

    @Test
    @DisplayName("Should return null when wallet does not exist during withdrawal")
    void testWithdrawWallet_WalletNotFound_ReturnsNull() {
        // Arrange
        WithdrawWalletRequest request = WithdrawWalletRequest.builder()
                .walletId(walletId)
                .amount(500.0)
                .destination("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> walletService.WithdrawWallet(request, testUserDetails));
        verify(transactionService, never()).CreateTransaction(any());
    }

    @Test
    @DisplayName("Should allow shopping payment when wallet is active for shopping")
    void testWithdrawWallet_ShoppingPaymentActiveWallet_Succeeds() {
        // Arrange
        double paymentAmount = 250.0;
        WithdrawWalletRequest request = WithdrawWalletRequest.builder()
                .walletId(walletId)
                .amount(paymentAmount)
                .destination("Store ABC")
                .oppositePartyType(OppositePartyType.PAYMENT)
                .build();

        Transaction expectedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(testWallet)
                .amount(paymentAmount)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Store ABC")
                .oppositePartyType(OppositePartyType.PAYMENT)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(transactionService.CreateTransaction(any(Transaction.class))).thenReturn(expectedTransaction);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        WithdrawWalletResponse response = walletService.WithdrawWallet(request, testUserDetails);

        // Assert
        assertNotNull(response);
        assertEquals(OppositePartyType.PAYMENT, response.getTransaction().getOppositePartyType());
        verify(transactionService, times(1)).CreateTransaction(any(Transaction.class));
    }

    // ============== Security Tests ==============

    @Test
    @DisplayName("Should allow user to withdraw from own wallet")
    void testWithdrawWallet_OwnWallet_WithdrawsSuccessfully() {
        // Arrange
        CustomUserDetails walletOwner = CustomUserDetails.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .username("owner")
                .password("password")
                .role(Role.CUSTOMER)
                .build();

        WithdrawWalletRequest request = WithdrawWalletRequest.builder()
                .walletId(walletId)
                .amount(500.0)
                .destination("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        Transaction expectedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(testWallet)
                .amount(500.0)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(transactionService.CreateTransaction(any(Transaction.class))).thenReturn(expectedTransaction);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        WithdrawWalletResponse response = walletService.WithdrawWallet(request, walletOwner);

        // Assert
        assertNotNull(response);
        assertFalse(response.getIsPendingTransaction());
        assertEquals(TransactionStatus.APPROVED, response.getTransaction().getStatus());
        verify(transactionService, times(1)).CreateTransaction(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw SecurityException when non-wallet-owner non-EMPLOYEE user tries to withdraw")
    void testWithdrawWallet_NonOwnerNonEmployee_ThrowsSecurityException() {
        // Arrange
        UUID differentCustomerId = UUID.randomUUID();
        CustomUserDetails differentCustomerUser = CustomUserDetails.builder()
                .id(UUID.randomUUID())
                .customerId(differentCustomerId)
                .username("other_customer")
                .password("password")
                .role(Role.CUSTOMER)
                .build();

        WithdrawWalletRequest request = WithdrawWalletRequest.builder()
                .walletId(walletId)
                .amount(500.0)
                .destination("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        assertThrows(SecurityException.class, () -> walletService.WithdrawWallet(request, differentCustomerUser));
    }

    @Test
    @DisplayName("Should allow EMPLOYEE user to create wallet for authenticated customer")
    void testCreateWallet_EmployeeUser_CreatesWalletSuccessfully() {
        // Arrange
        CreateWalletRequest request = CreateWalletRequest.builder()
                .customerId(customerId)
                .walletName("New Wallet")
                .currency(Currency.EUR)
                .isActiveShopping(true)
                .isActiveWithdraw(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        CreateWalletResponse response = walletService.CreateWallet(request, testUserDetails);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getWallet());
        verify(customerRepository, times(1)).findById(customerId);
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when customer not found during wallet creation")
    void testCreateWallet_CustomerNotFound_ThrowsNotFoundException() {
        // Arrange
        CreateWalletRequest request = CreateWalletRequest.builder()
                .customerId(customerId)
                .walletName("New Wallet")
                .currency(Currency.EUR)
                .isActiveShopping(true)
                .isActiveWithdraw(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> walletService.CreateWallet(request, testUserDetails));
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when wallet not found during withdrawal")
    void testWithdrawWallet_WalletNotFound_ThrowsNotFoundException() {
        // Arrange
        WithdrawWalletRequest request = WithdrawWalletRequest.builder()
                .walletId(walletId)
                .amount(500.0)
                .destination("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> walletService.WithdrawWallet(request, testUserDetails));
        verify(transactionService, never()).CreateTransaction(any());
    }
}

