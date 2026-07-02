/*
 * =============================================================================
 * CLASS: TaxDAOImpl
 * PACKAGE: DAO
 * =============================================================================
 * WHAT: File-backed TaxDAO — reads Data/Taxes.txt.
 *
 * Mirrors ProductDAOImpl structure: LinkedHashMap cache, case normalization (UPPERCASE keys).
 *
 * SPRING DI: XML bean id "taxDao" in applicationContext.xml (or @Repository with AppConfig).
 *
 * BUSINESS RULE ENFORCEMENT: Service calls getTax; null return → "State not supported."
 *
 * INTERVIEW EXPLANATION:
 * "TaxDAOImpl is the whitelist of states we operate in. The file is the source of truth
 *  for both abbreviation and rate used in calculations."
 * =============================================================================
 */
package DAO;

import Model.Tax;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TaxDAOImpl implements TaxDAO {

    private static final String DEFAULT_TAX_FILE = "Data/Taxes.txt";
    private static final String DELIMITER = ",";
    private final String taxFile;

    /** STATEFUL cache — keys are uppercase state abbreviations. */
    private final Map<String, Tax> taxes = new LinkedHashMap<>();

    public TaxDAOImpl() {
        this(DEFAULT_TAX_FILE);
    }

    public TaxDAOImpl(String taxFile) {
        this.taxFile = taxFile;
    }

    @Override
    public Tax getTax(String stateAbbreviation) throws FlooringPersistenceException {
        loadTaxes();
        if (stateAbbreviation == null) {
            return null;
        }
        return taxes.get(stateAbbreviation.trim().toUpperCase());
    }

    @Override
    public List<Tax> getAllTaxes() throws FlooringPersistenceException {
        loadTaxes();
        return new ArrayList<>(taxes.values());
    }

    private void loadTaxes() throws FlooringPersistenceException {
        taxes.clear();
        try (Scanner scanner = new Scanner(new BufferedReader(new FileReader(taxFile)))) {
            if (scanner.hasNextLine()) {
                scanner.nextLine(); // StateAbbreviation,StateName,TaxRate
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                Tax tax = unmarshallTax(line);
                taxes.put(tax.getStateAbbreviation().toUpperCase(), tax);
            }
        } catch (FileNotFoundException e) {
            throw new FlooringPersistenceException("Could not load taxes data.", e);
        }
    }

    private Tax unmarshallTax(String line) {
        String[] tokens = line.split(DELIMITER);
        Tax tax = new Tax();
        tax.setStateAbbreviation(tokens[0]);
        tax.setStateName(tokens[1]);
        tax.setTaxRate(new BigDecimal(tokens[2]));
        return tax;
    }
}
