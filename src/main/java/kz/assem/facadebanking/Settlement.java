package kz.assem.facadebanking;

import java.math.BigDecimal;

public final class Settlement {
    public enum Status { NEW, CONFIRMED, CANCELLED }

    private final String transferId;
    private final BigDecimal amount;
    private Status status;

    public Settlement(String transferId, BigDecimal amount, Status status) {
        this.transferId = transferId;
        this.amount = amount;
        this.status = status;
    }

    public void confirm() {
        if (status != Status.NEW) throw new BankingException("Settlement cannot be confirmed");
        status = Status.CONFIRMED;
    }

    public void cancel() {
        if (status != Status.NEW && status != Status.CONFIRMED) {
            throw new BankingException("Settlement cannot be cancelled");
        }
        status = Status.CANCELLED;
    }

    public String transferId() { return transferId; }
    public BigDecimal amount() { return amount; }
    public Status status() { return status; }

    @Override
    public String toString() {
        return transferId + " amount=" + amount + " status=" + status;
    }
}
