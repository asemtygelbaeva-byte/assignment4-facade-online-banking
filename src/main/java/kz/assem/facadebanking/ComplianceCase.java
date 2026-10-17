package kz.assem.facadebanking;

public final class ComplianceCase {
    public enum Status { NEW, CONNECTED, NOTES_SAVED, CANCELLED }

    private final String visitId;
    private Status status;
    private String notes;

    public ComplianceCase(String visitId, Status status, String notes) {
        this.visitId = visitId;
        this.status = status;
        this.notes = notes;
    }

    public void connect() {
        if (status != Status.NEW) throw new BankingException("Video session cannot be connected");
        status = Status.CONNECTED;
    }

    public void saveNotes(String notes) {
        if (status != Status.CONNECTED) throw new BankingException("Session notes cannot be saved yet");
        if (notes == null || notes.isBlank()) throw new InvalidTransferException("Session notes are required");
        this.notes = notes;
        status = Status.NOTES_SAVED;
    }

    public void cancel() {
        if (status != Status.CONNECTED && status != Status.NOTES_SAVED && status != Status.NEW) {
            throw new BankingException("Video session cannot be cancelled");
        }
        status = Status.CANCELLED;
        notes = "cancelled during compensation";
    }

    public String transferId() { return visitId; }
    public Status status() { return status; }
    public String reason() { return notes; }

    @Override
    public String toString() {
        return visitId + " video status=" + status + " notes=" + notes;
    }
}
