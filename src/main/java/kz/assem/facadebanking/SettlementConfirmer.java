package kz.assem.facadebanking;

import java.nio.file.Path;
import java.util.ArrayList;

public final class SettlementConfirmer {
    private final SettlementStore store;

    public SettlementConfirmer(Path directory) {
        this.store = new SettlementStore(directory);
    }

    public Settlement confirm(TransferRequest request) {
        Settlement settlement = new Settlement(request.id(), request.amount(), Settlement.Status.NEW);
        settlement.confirm();
        ArrayList<Settlement> settlements = new ArrayList<>(store.all());
        settlements.add(settlement);
        store.saveAll(settlements);
        return settlement;
    }

    public java.util.List<Settlement> settlements() { return store.all(); }
    public void clear() { store.clear(); }
}
