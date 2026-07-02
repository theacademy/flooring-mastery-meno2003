/*
 * =============================================================================
 * INTERFACE: TaxDAO
 * PACKAGE: DAO
 * =============================================================================
 * WHAT: Read-only access to state tax data from Data/Taxes.txt.
 *
 * BUSINESS RULE: Unsupported state → getTax returns null → service throws validation error.
 *
 * INTERVIEW EXPLANATION:
 * "TaxDAO answers whether we can do business in a state and what rate applies."
 * =============================================================================
 */
package DAO;

import Model.Tax;

import java.util.List;

public interface TaxDAO {

    /** Lookup by abbreviation; null if state not in file. */
    Tax getTax(String stateAbbreviation) throws FlooringPersistenceException;

    List<Tax> getAllTaxes() throws FlooringPersistenceException;
}
