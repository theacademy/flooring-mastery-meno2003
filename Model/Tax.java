/*
 * =============================================================================
 * CLASS: Tax
 * PACKAGE: Model
 * =============================================================================
 * WHAT: DTO for one supported state row in Data/Taxes.txt.
 *
 * WHY: OrderServiceImpl looks up tax by state abbreviation to validate service area and
 *      to apply the correct rate in calculations.
 *
 * BUSINESS RULE: Orders cannot be placed for states not present in Taxes.txt (enforced
 *                 when taxDao.getTax returns null).
 *
 * INTERVIEW EXPLANATION:
 * "Tax rate is stored as a percentage in the file — the service divides by 100 when
 *  computing dollar tax from material plus labor subtotal."
 * =============================================================================
 */
package Model;

import java.math.BigDecimal;

public class Tax {

    /** e.g. TX — normalized to uppercase in TaxDAOImpl map keys. */
    private String stateAbbreviation;

    /** Full state name for reference/display in data file. */
    private String stateName;

    /** Percent rate e.g. 10.00 means 10% — not a decimal fraction 0.10. */
    private BigDecimal taxRate;

    public String getStateAbbreviation() {
        return stateAbbreviation;
    }

    public void setStateAbbreviation(String stateAbbreviation) {
        this.stateAbbreviation = stateAbbreviation;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
}
