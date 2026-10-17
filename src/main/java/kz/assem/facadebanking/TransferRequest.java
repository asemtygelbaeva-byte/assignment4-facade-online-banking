package kz.assem.facadebanking;

public record TransferRequest(String id, String patientId, String clinician, String slot, String sessionNotes) {
}
