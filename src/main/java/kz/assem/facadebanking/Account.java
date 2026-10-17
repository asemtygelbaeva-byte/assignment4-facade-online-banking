package kz.assem.facadebanking;

public final class Account {
    public enum Status { REGISTERED, HELD, COMPLETED, CANCELLED }

    private final String id;
    private final String patientName;
    private String clinician;
    private String slot;
    private Status status;

    public Account(String id, String patientName, String clinician, String slot, Status status) {
        this.id = id;
        this.patientName = patientName;
        this.clinician = clinician;
        this.slot = slot;
        this.status = status;
    }

    public void hold(String clinician, String slot) {
        if (status != Status.REGISTERED) {
            throw new InvalidTransferException("Patient is not available for a new visit");
        }
        this.clinician = clinician;
        this.slot = slot;
        status = Status.HELD;
    }

    public void release() {
        if (status != Status.HELD) {
            throw new BankingException("Patient visit is not held");
        }
        clinician = "";
        slot = "";
        status = Status.REGISTERED;
    }

    public void complete() {
        if (status != Status.HELD) {
            throw new BankingException("Patient visit cannot be completed");
        }
        status = Status.COMPLETED;
    }

    public String id() { return id; }
    public String patientName() { return patientName; }
    public String clinician() { return clinician; }
    public String slot() { return slot; }
    public Status status() { return status; }

    @Override
    public String toString() {
        return id + " patient=" + patientName + " clinician=" + clinician
                + " slot=" + slot + " status=" + status;
    }
}
