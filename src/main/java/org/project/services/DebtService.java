package org.project.services;

import org.project.model.Debt;
import org.project.model.Expense;

import java.util.ArrayList;
import java.util.List;

public class DebtService {
    public List<Debt> calculateDebts(Expense expense){
        List<Debt> debts = new ArrayList<>();
        List<String> participants  = expense.getParticipants();
        String payer = expense.getPayer();
        double total = expense.getAmount();
        int count = participants.size();
        double baseShare = total/count;
        for (int i = 0; i < count; i++){
            String participant = participants.get(i);
            double share = baseShare;
            if(share == 0 || participant.equals(payer)){
                continue;
            }
            Debt debt = new Debt(expense,participant,payer,share);
            debts.add(debt);
        }
        return debts;
    }
}
