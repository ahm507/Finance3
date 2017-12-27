package org.pf.service;

import org.pf.domain.enumeration.AccountType;
import org.pf.service.dto.TransactionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service Interface for managing Transaction.
 */
public interface TransactionService {

    /**
     * Save a transaction.
     *
     * @param transactionDTO the entity to save
     * @return the persisted entity
     */
    TransactionDTO save(TransactionDTO transactionDTO);

    /**
     * Get all the transactions.
     *
     * @return the list of entities
     */
    List<TransactionDTO> findAll();

    public List<TransactionDTO> findByUserLoginAndAccountId(String lgin, long userAccountId);

    /**
     * Get the "id" transaction.
     *
     * @param id the id of the entity
     * @return the entity
     */
    TransactionDTO findOne(Long id);

    /**
     * Delete the "id" transaction.
     *
     * @param id the id of the entity
     */
    void delete(Long id);

    /**
     * Search for the transaction corresponding to the query.
     *
     * @param query the query of the search
     * @return the list of entities
     */
    Page<TransactionDTO> search(String query, Pageable pageable);

    public List<String> getYearList(String login);

    public List<TransactionDTO> findYearTransactions(String login, Long userAccountId, Long year);

    boolean isInvalidCurrencies(TransactionDTO transactionDTO);

    double computeBalance(long accountId, AccountType type, double initialBalance,
        List<TransactionDTO> listOfTrans);

}
