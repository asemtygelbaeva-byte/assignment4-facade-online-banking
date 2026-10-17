package kz.assem.facadebanking;

import java.math.BigDecimal;

public record ChainRecord(String id, String source, String target, BigDecimal amount, Stage stage, String message) {
    public enum Stage { PENDING_B, PENDING_C, COMPLETED, COMPENSATED_A, COMPENSATED_B_A }
}
