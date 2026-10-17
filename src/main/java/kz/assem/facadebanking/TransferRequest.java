package kz.assem.facadebanking;

import java.math.BigDecimal;

public record TransferRequest(String id, String sourceAccount, String targetAccount, BigDecimal amount) {
}
