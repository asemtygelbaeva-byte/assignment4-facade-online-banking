package kz.assem.facadebanking;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Scanner;

public final class BankingCli {
    private BankingCli() { }

    public static void main(String[] args) {
        Path data = args.length == 0 ? Path.of("data") : Path.of(args[0]);
        AccountLedger ledger = new AccountLedger(data);
        ComplianceChecker compliance = new ComplianceChecker(data);
        SettlementConfirmer settlement = new SettlementConfirmer(data);
        ChainStore chains = new ChainStore(data);
        BankingFacade banking = new BankingFacade(ledger, compliance, settlement, chains);
        OperationsFacade operations = new OperationsFacade(banking, ledger, compliance, settlement);
        ledger.seedDefaults();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Online Banking Facade demo");
            boolean running = true;
            while (running) {
                printMenu();
                String command = scanner.nextLine().trim();
                try {
                    switch (command) {
                        case "1" -> runTransfer(scanner, banking, FailurePoint.NONE);
                        case "2" -> runTransfer(scanner, banking, FailurePoint.B);
                        case "3" -> runTransfer(scanner, banking, FailurePoint.C);
                        case "4" -> System.out.println(operations.inspect());
                        case "5" -> startOnly(scanner, banking);
                        case "6" -> resume(scanner, banking);
                        case "7" -> operations.manuallyCompensate(read(scanner, "Transfer id"));
                        case "8" -> { operations.reset(); ledger.seedDefaults(); System.out.println("Store reset."); }
                        case "0" -> running = false;
                        default -> System.out.println("Choose 0-8.");
                    }
                } catch (BankingException exception) {
                    System.out.println("Result: " + exception.getMessage());
                }
            }
        }
    }

    private static void runTransfer(Scanner scanner, BankingFacade banking, FailurePoint failurePoint) {
        TransferRequest request = request(scanner);
        banking.execute(request, failurePoint);
        System.out.println("A-B-C completed: " + request.id());
    }

    private static void startOnly(Scanner scanner, BankingFacade banking) {
        TransferRequest request = request(scanner);
        banking.start(request);
        System.out.println("A committed. Restart the app, then use option 6 with " + request.id());
    }

    private static void resume(Scanner scanner, BankingFacade banking) {
        banking.resume(read(scanner, "Transfer id"), FailurePoint.NONE);
        System.out.println("Pending transfer resumed.");
    }

    private static TransferRequest request(Scanner scanner) {
        String id = read(scanner, "Transfer id (example tx-1)");
        String amount = read(scanner, "Amount");
        return new TransferRequest(id, "alice", "bob", new BigDecimal(amount));
    }

    private static String read(Scanner scanner, String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private static void printMenu() {
        System.out.println("1 success  2 fail B (A compensation)  3 fail C (B then A compensation)");
        System.out.println("4 inspect persisted state  5 stop after A  6 resume  7 manual compensation  8 reset  0 exit");
        System.out.print("> ");
    }
}
