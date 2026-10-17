package kz.assem.facadebanking;

public final class Settlement {
    public enum Status { NEW, PRESCRIPTION_SENT, BILLED, CANCELLED }

    private final String transferId;
    private final String clinician;
    private final String slot;
    private Status status;

    public Settlement(String transferId, String clinician, String slot, Status status) {
        this.transferId = transferId;
        this.clinician = clinician;
        this.slot = slot;
        this.status = status;
    }

    public void confirm() {
        if (status != Status.NEW) throw new BankingException("E-prescription cannot be sent");
        status = Status.PRESCRIPTION_SENT;
        status = Status.BILLED;
    }

    public void cancel() {
        if (status != Status.NEW && status != Status.PRESCRIPTION_SENT) {
            throw new BankingException("Prescription billing cannot be cancelled");
        }
        status = Status.CANCELLED;
    }

    public String transferId() { return transferId; }
    public String clinician() { return clinician; }
    public String slot() { return slot; }
    public Status status() { return status; }

    @Override
    public String toString() {
        return transferId + " clinician=" + clinician + " slot=" + slot + " status=" + status;
    }
}
