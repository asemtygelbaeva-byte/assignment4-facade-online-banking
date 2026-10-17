package kz.assem.facadebanking;

public class SubsystemFailureException extends BankingException {
    public SubsystemFailureException(String message) {
        super(message);
    }
}
