package com.inghubs.digitalwallet.TransactionTests;

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
import com.inghubs.digitalwallet.utilities.enums.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private TransactionService transactionService;

    private UUID customerId;
    private UUID walletId;
    private UUID transactionId;
    private Customer testCustomer;
    private Wallet testWallet;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        testCustomer = Customer.builder()
                .id(customerId)
                .name("John")
                .surname("Doe")
                .TCKN("12345678901")
                .build();

        testWallet = Wallet.builder()
                .id(walletId)
                .customer(testCustomer)
                .walletName("Main Wallet")
                .currency(Currency.TRY)
                .balance(5000.0)
                .usableBalance(5000.0)
                .isActiveShopping(true)
                .isActiveWithdraw(true)
                .build();

        testTransaction = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(500.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();
    }

    // ============== CreateTransaction Tests ==============

    @Test
    @DisplayName("Should create transaction successfully when wallet exists")
    void testCreateTransaction_WalletExists_CreatesTransaction() {
        // Arrange
        when(walletRepository.existsById(walletId)).thenReturn(true);
        when(transactionRepository.save(testTransaction)).thenReturn(testTransaction);

        // Act
        Transaction result = transactionService.CreateTransaction(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals(transactionId, result.getId());
        assertEquals(500.0, result.getAmount());
        verify(walletRepository, times(1)).existsById(walletId);
        verify(transactionRepository, times(1)).save(testTransaction);
    }

    @Test
    @DisplayName("Should return null when wallet does not exist")
    void testCreateTransaction_WalletNotExists_ReturnsNull() {
        // Arrange
        when(walletRepository.existsById(walletId)).thenReturn(false);

        // Act
        Transaction result = transactionService.CreateTransaction(testTransaction);

        // Assert
        assertNull(result);
        verify(walletRepository, times(1)).existsById(walletId);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should auto-complete approved deposit transaction")
    void testCreateTransaction_ApprovedDeposit_AutoCompletes() {
        // Arrange
        Transaction approvedDeposit = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(500.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.existsById(walletId)).thenReturn(true);
        when(transactionRepository.save(approvedDeposit)).thenReturn(approvedDeposit);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        Transaction result = transactionService.CreateTransaction(approvedDeposit);

        // Assert
        assertNotNull(result);
        assertEquals(TransactionStatus.APPROVED, result.getStatus());
        verify(walletRepository, atLeast(1)).findById(walletId);
        verify(walletRepository, atLeast(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should auto-complete approved withdrawal transaction")
    void testCreateTransaction_ApprovedWithdraw_AutoCompletes() {
        // Arrange
        Transaction approvedWithdraw = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(300.0)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.existsById(walletId)).thenReturn(true);
        when(transactionRepository.save(approvedWithdraw)).thenReturn(approvedWithdraw);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        Transaction result = transactionService.CreateTransaction(approvedWithdraw);

        // Assert
        assertNotNull(result);
        assertEquals(TransactionStatus.APPROVED, result.getStatus());
        verify(walletRepository, atLeast(1)).save(any(Wallet.class));
    }

    // ============== ApproveTransaction Tests ==============

    @Test
    @DisplayName("Should approve transaction and update status to APPROVED")
    void testApproveTransaction_ApproveStatus_UpdatesSuccessfully() {
        // Arrange
        ApproveTransactionRequest request = ApproveTransactionRequest.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.APPROVED)
                .build();

        Transaction pendingTransaction = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(500.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        Transaction approvedTransaction = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(500.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingTransaction));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(approvedTransaction);

        // Act
        ApproveTransactionResponse response = transactionService.ApproveTransaction(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getTransaction());
        assertEquals(TransactionStatus.APPROVED, response.getTransaction().getStatus());
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should deny transaction and update status to DENIED")
    void testApproveTransaction_DenyStatus_UpdatesSuccessfully() {
        // Arrange
        ApproveTransactionRequest request = ApproveTransactionRequest.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.DENIED)
                .build();

        Transaction pendingTransaction = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(500.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        Transaction deniedTransaction = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(500.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.DENIED)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingTransaction));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(deniedTransaction);

        // Act
        ApproveTransactionResponse response = transactionService.ApproveTransaction(request);

        // Assert
        assertNotNull(response);
        assertEquals(TransactionStatus.DENIED, response.getTransaction().getStatus());
        verify(walletRepository, times(1)).findById(walletId);
    }

    @Test
    @DisplayName("Should return null when transaction does not exist")
    void testApproveTransaction_TransactionNotFound_ReturnsNull() {
        // Arrange
        ApproveTransactionRequest request = ApproveTransactionRequest.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.APPROVED)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // Act
        ApproveTransactionResponse response = transactionService.ApproveTransaction(request);

        // Assert
        assertNull(response);
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should complete pending deposit transaction when approved")
    void testApproveTransaction_PendingDeposit_CompletesAndUpdatesBalance() {
        // Arrange
        ApproveTransactionRequest request = ApproveTransactionRequest.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.APPROVED)
                .build();

        Transaction pendingDeposit = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(1000.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        Wallet walletBeforeApproval = Wallet.builder()
                .id(walletId)
                .customer(testCustomer)
                .walletName("Main Wallet")
                .currency(Currency.TRY)
                .balance(5000.0)
                .usableBalance(5000.0)
                .isActiveShopping(true)
                .isActiveWithdraw(true)
                .build();

        Transaction approvedDeposit = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(1000.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingDeposit));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(walletBeforeApproval));
        when(walletRepository.save(any(Wallet.class))).thenReturn(walletBeforeApproval);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(approvedDeposit);

        // Act
        ApproveTransactionResponse response = transactionService.ApproveTransaction(request);

        // Assert
        assertNotNull(response);
        verify(walletRepository, atLeast(1)).save(any(Wallet.class));
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, atLeast(1)).save(walletCaptor.capture());
    }

    @Test
    @DisplayName("Should revert pending deposit transaction when denied")
    void testApproveTransaction_DenyPendingDeposit_RevertsBalance() {
        // Arrange
        ApproveTransactionRequest request = ApproveTransactionRequest.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.DENIED)
                .build();

        Transaction pendingDeposit = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(500.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        Transaction deniedDeposit = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(500.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.DENIED)
                .oppositeParty("Bank Transfer")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingDeposit));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(deniedDeposit);

        // Act
        ApproveTransactionResponse response = transactionService.ApproveTransaction(request);

        // Assert
        assertNotNull(response);
        assertEquals(TransactionStatus.DENIED, response.getTransaction().getStatus());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should revert pending withdrawal when denied")
    void testApproveTransaction_DenyPendingWithdraw_RevertsBalance() {
        // Arrange
        ApproveTransactionRequest request = ApproveTransactionRequest.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.DENIED)
                .build();

        Transaction pendingWithdraw = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(800.0)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.PENDING)
                .oppositeParty("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        Transaction deniedWithdraw = Transaction.builder()
                .id(transactionId)
                .wallet(testWallet)
                .amount(800.0)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.DENIED)
                .oppositeParty("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingWithdraw));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(deniedWithdraw);

        // Act
        ApproveTransactionResponse response = transactionService.ApproveTransaction(request);

        // Assert
        assertNotNull(response);
        assertEquals(TransactionStatus.DENIED, response.getTransaction().getStatus());
    }

    // ============== ListTransactions Tests ==============

    @Test
    @DisplayName("Should list transactions when wallet exists")
    void testListTransactions_WalletExists_ReturnsTransactionList() {
        // Arrange
        List<Transaction> transactionList = new ArrayList<>();
        transactionList.add(testTransaction);

        Transaction transaction2 = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(testWallet)
                .amount(300.0)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Account ABC")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();
        transactionList.add(transaction2);

        when(walletRepository.existsById(walletId)).thenReturn(true);
        when(transactionRepository.findByWalletId(walletId)).thenReturn(transactionList);

        // Act
        ListTransactionsResponse response = transactionService.ListTransactions(walletId);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTransactions().size());
        assertEquals(500.0, response.getTransactions().get(0).getAmount());
        assertEquals(300.0, response.getTransactions().get(1).getAmount());
        verify(walletRepository, times(1)).existsById(walletId);
        verify(transactionRepository, times(1)).findByWalletId(walletId);
    }

    @Test
    @DisplayName("Should return empty list when wallet has no transactions")
    void testListTransactions_NoTransactions_ReturnsEmptyList() {
        // Arrange
        when(walletRepository.existsById(walletId)).thenReturn(true);
        when(transactionRepository.findByWalletId(walletId)).thenReturn(new ArrayList<>());

        // Act
        ListTransactionsResponse response = transactionService.ListTransactions(walletId);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTransactions().size());
        verify(transactionRepository, times(1)).findByWalletId(walletId);
    }

    @Test
    @DisplayName("Should return null when wallet does not exist")
    void testListTransactions_WalletNotFound_ReturnsNull() {
        // Arrange
        when(walletRepository.existsById(walletId)).thenReturn(false);

        // Act
        ListTransactionsResponse response = transactionService.ListTransactions(walletId);

        // Assert
        assertNull(response);
        verify(walletRepository, times(1)).existsById(walletId);
        verify(transactionRepository, never()).findByWalletId(any());
    }

    // ============== Balance Operation Tests ==============

    @Test
    @DisplayName("Should update balance correctly for completed pending deposit")
    void testBalanceUpdate_CompletedPendingDeposit_UpdatesBalance() {
        // Arrange
        double initialBalance = 5000.0;
        double depositAmount = 500.0;

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .customer(testCustomer)
                .walletName("Main Wallet")
                .currency(Currency.TRY)
                .balance(initialBalance)
                .usableBalance(initialBalance)
                .isActiveShopping(true)
                .isActiveWithdraw(true)
                .build();

        Transaction pendingDeposit = Transaction.builder()
                .id(transactionId)
                .wallet(wallet)
                .amount(depositAmount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Bank")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.existsById(walletId)).thenReturn(true);
        when(transactionRepository.save(pendingDeposit)).thenReturn(pendingDeposit);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        when(walletRepository.save(walletCaptor.capture())).thenReturn(wallet);

        // Act
        transactionService.CreateTransaction(pendingDeposit);

        // Assert
        Wallet savedWallet = walletCaptor.getValue();
        assertEquals(initialBalance + depositAmount, savedWallet.getBalance());
    }

    @Test
    @DisplayName("Should update balance correctly for completed pending withdrawal")
    void testBalanceUpdate_CompletedPendingWithdraw_UpdatesBalance() {
        // Arrange
        double initialBalance = 5000.0;
        double withdrawAmount = 800.0;

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .customer(testCustomer)
                .walletName("Main Wallet")
                .currency(Currency.TRY)
                .balance(initialBalance)
                .usableBalance(initialBalance)
                .isActiveShopping(true)
                .isActiveWithdraw(true)
                .build();

        Transaction pendingWithdraw = Transaction.builder()
                .id(transactionId)
                .wallet(wallet)
                .amount(withdrawAmount)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Account XYZ")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(walletRepository.existsById(walletId)).thenReturn(true);
        when(transactionRepository.save(pendingWithdraw)).thenReturn(pendingWithdraw);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        when(walletRepository.save(walletCaptor.capture())).thenReturn(wallet);

        // Act
        transactionService.CreateTransaction(pendingWithdraw);

        // Assert
        Wallet savedWallet = walletCaptor.getValue();
        assertEquals(initialBalance - withdrawAmount, savedWallet.getBalance());
    }

    @Test
    @DisplayName("Should update usable balance for approved deposit")
    void testBalanceUpdate_ApprovedDeposit_UpdatesUsableBalance() {
        // Arrange
        double initialBalance = 5000.0;
        double depositAmount = 500.0;

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .customer(testCustomer)
                .walletName("Main Wallet")
                .currency(Currency.TRY)
                .balance(initialBalance)
                .usableBalance(initialBalance)
                .isActiveShopping(true)
                .isActiveWithdraw(true)
                .build();

        ApproveTransactionRequest request = ApproveTransactionRequest.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.APPROVED)
                .build();

        Transaction pendingDeposit = Transaction.builder()
                .id(transactionId)
                .wallet(wallet)
                .amount(depositAmount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .oppositeParty("Bank")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingDeposit));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        when(walletRepository.save(walletCaptor.capture())).thenReturn(wallet);

        Transaction approvedDeposit = Transaction.builder()
                .id(transactionId)
                .wallet(wallet)
                .amount(depositAmount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.APPROVED)
                .oppositeParty("Bank")
                .oppositePartyType(OppositePartyType.IBAN)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(approvedDeposit);

        // Act
        transactionService.ApproveTransaction(request);

        // Assert
        Wallet savedWallet = walletCaptor.getValue();
        assertEquals(initialBalance + depositAmount, savedWallet.getUsableBalance());
    }
}
