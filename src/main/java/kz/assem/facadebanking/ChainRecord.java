package kz.assem.facadebanking;

public record ChainRecord(String id, String patientId, String clinician, String slot,
                          String sessionNotes, Stage stage, String message) {
    public enum Stage { PENDING_B, PENDING_C, COMPLETED, COMPENSATED_A, COMPENSATED_B_A }
}
