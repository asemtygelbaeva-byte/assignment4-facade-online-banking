package kz.assem.facadebanking;

import java.math.BigDecimal;

public final class Account {
    public enum Status { ACTIVE, RESERVED, COMPLETED, CANCELLED }

    private final String id;
    private BigDecimal balance;
    private BigDecimal reserved;
    private Status status;

    public Account(String id, BigDecimal balance, BigDecimal reserved, Status status) {
        this.id = id;
        this.balance = balance;
        this.reserved = reserved;
        this.status = status;
    }

    public void reserve(BigDecimal amount) {
        if (status == Status.CANCELLED || balance.subtract(reserved).compareTo(amount) < 0) {
            throw new InvalidTransferException("Account cannot reserve " + amount);
        }
        reserved = reserved.add(amount);
        status = Status.RESERVED;
    }

    public void release(BigDecimal amount) {
        if (status != Status.RESERVED || reserved.compareTo(amount) < 0) {
            throw new BankingException("Account has no matching reservation");
        }
        reserved = reserved.subtract(amount);
        status = Status.ACTIVE;
    }

    public void complete(BigDecimal amount) {
        if (status != Status.RESERVED || reserved.compareTo(amount) < 0) {
            throw new BankingException("Account cannot complete this transfer");
        }
        reserved = reserved.subtract(amount);
        balance = balance.subtract(amount);
        status = Status.COMPLETED;
    }

    public void credit(BigDecimal amount) {
        if (status == Status.CANCELLED) {
            throw new BankingException("Cancelled account cannot receive money");
        }
        balance = balance.add(amount);
        status = Status.COMPLETED;
    }

    public String id() { return id; }
    public BigDecimal balance() { return balance; }
    public BigDecimal reserved() { return reserved; }
    public Status status() { return status; }

    @Override
    public String toString() {
        return id + " balance=" + balance + " reserved=" + reserved + " status=" + status;
    }
}
