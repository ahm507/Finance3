package org.pf.web.rest;

import com.codahale.metrics.annotation.Timed;
import io.github.jhipster.web.util.ResponseUtil;
import org.pf.security.SecurityUtils;
import org.pf.service.CurrencyService;
import org.pf.service.dto.CurrencyDTO;
import org.pf.web.rest.errors.BadRequestAlertException;
import org.pf.web.rest.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Currency.
 */
@RestController
@RequestMapping("/api")
public class CurrencyResource {

    private final Logger log = LoggerFactory.getLogger(CurrencyResource.class);

    private static final String ENTITY_NAME = "currency";

    private final CurrencyService currencyService;

    public CurrencyResource(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    /**
     * POST  /currencies : Create a new currency.
     *
     * @param currencyDTO the currencyDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new currencyDTO, or with status 400 (Bad Request) if the currency has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/currencies")
    @Timed
    public ResponseEntity<CurrencyDTO> createCurrency(@Valid @RequestBody CurrencyDTO currencyDTO) throws URISyntaxException {
        log.debug("REST request to save Currency : {}", currencyDTO);
        if (currencyDTO.getId() != null) {
            throw new BadRequestAlertException("A new currency cannot already have an ID", ENTITY_NAME, "idexists");
        }

        if(currencyService.isDuplicateName(currencyDTO.getUserLogin(), currencyDTO.getName())) {
            throw new BadRequestAlertException("Duplicate currency name ", ENTITY_NAME, "value.duplicate");
        }

        CurrencyDTO result = currencyService.save(currencyDTO);
        return ResponseEntity.created(new URI("/api/currencies/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /currencies : Updates an existing currency.
     *
     * @param currencyDTO the currencyDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated currencyDTO,
     * or with status 400 (Bad Request) if the currencyDTO is not valid,
     * or with status 500 (Internal Server Error) if the currencyDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/currencies")
    @Timed
    public ResponseEntity<CurrencyDTO> updateCurrency(@Valid @RequestBody CurrencyDTO currencyDTO) throws URISyntaxException {
        log.debug("REST request to update Currency : {}", currencyDTO);
        if (currencyDTO.getId() == null) {
            return createCurrency(currencyDTO);
        }
        CurrencyDTO result = currencyService.save(currencyDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, currencyDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /currencies : get all the currencies.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of currencies in body
     */
    @GetMapping("/currencies")
    @Timed
    public List<CurrencyDTO> getAllCurrencies(@RequestParam(required = false, value = "login")  String login) {
        log.debug("REST request to get all Currencies");
        if(login == null) { //WEB Currently ONLY.
            login = SecurityUtils.getCurrentUserLogin().get();
        }
        //Unit Test
        return currencyService.findAllByCurrentUser(login);
        }

    /**
     * GET  /currencies/:id : get the "id" currency.
     *
     * @param id the id of the currencyDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the currencyDTO, or with status 404 (Not Found)
     */
    @GetMapping("/currencies/{id}")
    @Timed
    public ResponseEntity<CurrencyDTO> getCurrency(@PathVariable Long id) {
        log.debug("REST request to get Currency : {}", id);
        CurrencyDTO currencyDTO = currencyService.findOne(id);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(currencyDTO));
    }

    /**
     * DELETE  /currencies/:id : delete the "id" currency.
     *
     * @param id the id of the currencyDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/currencies/{id}")
    @Timed
    public ResponseEntity<Void> deleteCurrency(@PathVariable Long id) {
        log.debug("REST request to delete Currency : {}", id);
        currencyService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

}
