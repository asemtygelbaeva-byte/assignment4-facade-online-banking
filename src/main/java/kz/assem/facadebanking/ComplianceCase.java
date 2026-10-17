package kz.assem.facadebanking;

public final class ComplianceCase {
    public enum Status { NEW, APPROVED, CANCELLED, REJECTED }

    private final String transferId;
    private Status status;
    private String reason;

    public ComplianceCase(String transferId, Status status, String reason) {
        this.transferId = transferId;
        this.status = status;
        this.reason = reason;
    }

    public void approve() {
        if (status != Status.NEW) throw new BankingException("Compliance case cannot be approved");
        status = Status.APPROVED;
        reason = "approved";
    }

    public void cancel() {
        if (status != Status.APPROVED && status != Status.NEW) {
            throw new BankingException("Compliance case cannot be cancelled");
        }
        status = Status.CANCELLED;
        reason = "compensated";
    }

    public String transferId() { return transferId; }
    public Status status() { return status; }
    public String reason() { return reason; }

    @Override
    public String toString() {
        return transferId + " status=" + status + " reason=" + reason;
    }
}
