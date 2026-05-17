package Practice.FlooringMastery.DAO;

import Practice.FlooringMastery.Model.Tax;

import java.util.List;

public interface TaxDAO {
    Tax getTax(String stateAbbreviation) throws FlooringPersistenceException;

    List<Tax> getAllTaxes() throws FlooringPersistenceException;
}
