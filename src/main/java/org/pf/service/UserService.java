package org.pf.service;

import org.pf.config.Constants;
import org.pf.domain.Authority;
import org.pf.domain.Currency;
import org.pf.domain.User;
import org.pf.repository.AuthorityRepository;
import org.pf.repository.CurrencyRepository;
import org.pf.repository.PersistentTokenRepository;
import org.pf.repository.UserRepository;
import org.pf.repository.search.UserSearchRepository;
import org.pf.security.AuthoritiesConstants;
import org.pf.security.SecurityUtils;
import org.pf.service.dto.UserDTO;
import org.pf.service.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final String USERS_CACHE = "users";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final SocialService socialService;

    private final UserSearchRepository userSearchRepository;

    private final PersistentTokenRepository persistentTokenRepository;

    private final AuthorityRepository authorityRepository;

    private final CacheManager cacheManager;

    private final CurrencyRepository currencyRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, SocialService socialService,
        UserSearchRepository userSearchRepository, PersistentTokenRepository persistentTokenRepository,
        AuthorityRepository authorityRepository, CacheManager cacheManager, CurrencyRepository currencyRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.socialService = socialService;
        this.userSearchRepository = userSearchRepository;
        this.persistentTokenRepository = persistentTokenRepository;
        this.authorityRepository = authorityRepository;
        this.cacheManager = cacheManager;
        this.currencyRepository = currencyRepository;
    }

    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository.findOneByActivationKey(key)
            .map(user -> {
                // activate given user for the registration key.
                user.setActivated(true);
                user.setActivationKey(null);
                userSearchRepository.save(user);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                log.debug("Activated user: {}", user);
                return user;
            });
    }

    public Optional<User> completePasswordReset(String newPassword, String key) {
       log.debug("Reset user password for reset key {}", key);

       return userRepository.findOneByResetKey(key)
           .filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400)))
           .map(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetKey(null);
                user.setResetDate(null);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                return user;
           });
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository.findOneByEmailIgnoreCase(mail)
            .filter(User::getActivated)
            .map(user -> {
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(Instant.now());
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                return user;
            });
    }

    public User registerUser(UserDTO userDTO, String password) {

        User newUser = new User();
        Authority authority = authorityRepository.findOne(AuthoritiesConstants.USER);
        Set<Authority> authorities = new HashSet<>();
        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setLogin(userDTO.getLogin());
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userDTO.getFirstName());
        newUser.setLastName(userDTO.getLastName());
        newUser.setEmail(userDTO.getEmail());
        newUser.setImageUrl(userDTO.getImageUrl());
        newUser.setLangKey(userDTO.getLangKey());
        newUser.setMasterCurrency(1L); // This is initial ID that is wrong, but used only to enable initial saving.
        // new user is not active
        newUser.setActivated(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        authorities.add(authority);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);
        createInitialCurrency(newUser);
        userSearchRepository.save(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setLogin(userDTO.getLogin());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setImageUrl(userDTO.getImageUrl());
        if (userDTO.getLangKey() == null) {
            user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
        } else {
            user.setLangKey(userDTO.getLangKey());
        }
        user.setMasterCurrency(1L); // This is initial ID that is wrong, but used only to enable initial saving.
        if (userDTO.getAuthorities() != null) {
            Set<Authority> authorities = userDTO.getAuthorities().stream()
                .map(authorityRepository::findOne)
                .collect(Collectors.toSet());
            user.setAuthorities(authorities);
        }
        String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        user.setActivated(true);
        userRepository.save(user);
        createInitialCurrency(user);
        userSearchRepository.save(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }

    private void createInitialCurrency(User user) {
        Currency initialCurrency = new Currency();
        initialCurrency.setName("EGP");
        initialCurrency.setConversionRate(1D);
        initialCurrency.setUser(user);
        currencyRepository.save(initialCurrency);
        //create currency
        user.setMasterCurrency(initialCurrency.getId());
        //update user.masterCurrency to the created one.
        userRepository.save(user);
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param firstName first name of user
     * @param lastName last name of user
     * @param email email id of user
     * @param langKey language key
     * @param imageUrl image URL of user
     */
    public void updateUser(String firstName, String lastName, String email, String langKey, String imageUrl, Long masterCurrency) {
        SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .ifPresent(user -> {
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setEmail(email);
                user.setLangKey(langKey);
                user.setImageUrl(imageUrl);
                user.setMasterCurrency(masterCurrency);
                userSearchRepository.save(user);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                log.debug("Changed Information for User: {}", user);
            });
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update
     * @return updated user
     */
    public Optional<UserDTO> updateUser(UserDTO userDTO) {
        return Optional.of(userRepository
            .findOne(userDTO.getId()))
            .map(user -> {
                user.setLogin(userDTO.getLogin());
                user.setFirstName(userDTO.getFirstName());
                user.setLastName(userDTO.getLastName());
                user.setEmail(userDTO.getEmail());
                user.setImageUrl(userDTO.getImageUrl());
                user.setActivated(userDTO.isActivated());
                user.setLangKey(userDTO.getLangKey());
                user.setMasterCurrency(userDTO.getMasterCurrency());
                Set<Authority> managedAuthorities = user.getAuthorities();
                managedAuthorities.clear();
                userDTO.getAuthorities().stream()
                    .map(authorityRepository::findOne)
                    .forEach(managedAuthorities::add);
                userSearchRepository.save(user);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                log.debug("Changed Information for User: {}", user);
                return user;
            })
            .map(UserDTO::new);
    }

    public void deleteUser(String login) {
        userRepository.findOneByLogin(login).ifPresent(user -> {
            socialService.deleteUserSocialConnection(user.getLogin());
            userRepository.delete(user);
            userSearchRepository.delete(user);
            cacheManager.getCache(USERS_CACHE).evict(login);
            log.debug("Deleted User: {}", user);
        });
    }

    public void changePassword(String password) {
        SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .ifPresent(user -> {
                String encryptedPassword = passwordEncoder.encode(password);
                user.setPassword(encryptedPassword);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                log.debug("Changed password for User: {}", user);
            });
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAllByLoginNot(pageable, Constants.ANONYMOUS_USER).map(UserDTO::new);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByLogin(login);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities(Long id) {
        return userRepository.findOneWithAuthoritiesById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin);
    }

    /**
     * Persistent Token are used for providing automatic authentication, they should be automatically deleted after
     * 30 days.
     * <p>
     * This is scheduled to get fired everyday, at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void removeOldPersistentTokens() {
        LocalDate now = LocalDate.now();
        persistentTokenRepository.findByTokenDateBefore(now.minusMonths(1)).forEach(token -> {
            log.debug("Deleting token {}", token.getSeries());
            User user = token.getUser();
            user.getPersistentTokens().remove(token);
            persistentTokenRepository.delete(token);
        });
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        List<User> users = userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(Instant.now().minus(3, ChronoUnit.DAYS));
        for (User user : users) {
            log.debug("Deleting not activated user {}", user.getLogin());
            userRepository.delete(user);
            userSearchRepository.delete(user);
            cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
        }
    }

    /**
     * @return a list of all the authorities
     */
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).collect(Collectors.toList());
    }

}
