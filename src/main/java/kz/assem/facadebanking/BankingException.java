package kz.assem.facadebanking;

public class BankingException extends RuntimeException {
    public BankingException(String message) {
        super(message);
    }
}
