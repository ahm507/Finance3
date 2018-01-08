package org.pf.service;

import org.pf.service.dto.UserAccountDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service Interface for managing UserAccount.
 */
public interface UserAccountService {

    /**
     * Save a userAccount.
     *
     * @param userAccountDTO the entity to save
     * @return the persisted entity
     */
    UserAccountDTO save(UserAccountDTO userAccountDTO);

    /**
     * Get all the userAccounts.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    Page<UserAccountDTO> findAll(Pageable pageable);

    /**
     * Get the "id" userAccount.
     *
     * @param id the id of the entity
     * @return the entity
     */
    UserAccountDTO findOne(Long id);

    /**
     * Delete the "id" userAccount.
     *
     * @param id the id of the entity
     */
    void delete(Long id);

    Page<UserAccountDTO> findByCurrentUser(String login, Pageable pageable);

    List<UserAccountDTO> findByCurrentUser(String login);

    boolean isDuplicateName(String login, String accountText) ;

}
