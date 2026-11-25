package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.model.ExpenseShare;
import hu.unideb.inf.szoftverfejlesztes.model.RecurringExpense;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.RecurringExpenseRepository;
import hu.unideb.inf.szoftverfejlesztes.services.BudgetService.PreparedExpense;
import hu.unideb.inf.szoftverfejlesztes.services.GroupService.GroupAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

// Havonta ismétlődő kiadások: szabály létrehozása, leállítása, és a esedékes kiadások automatikus létrehozása
@Service
public class RecurringExpenseService {

    private static final Logger log = LoggerFactory.getLogger(RecurringExpenseService.class);
    // Ennyi hónapot pótol be egyszerre, ha sokáig nem futott az alkalmazás
    private static final int MAX_CATCH_UP_MONTHS = 24;

    private final RecurringExpenseRepository recurringRepository;
    private final BudgetService budgetService;
    private final GroupService groupService;

    public RecurringExpenseService(RecurringExpenseRepository recurringRepository, BudgetService budgetService,
                                   GroupService groupService) {
        this.recurringRepository = recurringRepository;
        this.budgetService = budgetService;
        this.groupService = groupService;
    }

    public List<RecurringExpense> getRecurringExpenses(User currentUser, Long groupId) {
        groupService.requireAccess(currentUser, groupId);
        return recurringRepository.findAllByGroupIdOrderByNextRunDateAsc(groupId);
    }

    // Új ismétlődő kiadás: a szabály mellett az első kiadás azonnal létrejön,
    // a következő a következő hónap megadott napján
    @Transactional
    public RecurringExpense create(User currentUser, Long groupId, ExpenseInput input, Integer dayOfMonth) {
        GroupAccess access = groupService.requireAccess(currentUser, groupId);
        int day = dayOfMonth == null ? Math.min(LocalDate.now().getDayOfMonth(), 28) : dayOfMonth;
        if (day < 1 || day > 28) {
            throw new IllegalArgumentException("Az ismétlődés napja 1 és 28 között lehet!");
        }
        PreparedExpense prepared = budgetService.prepareExpense(access, input);

        RecurringExpense rule = new RecurringExpense();
        rule.setGroup(access.group());
        rule.setTitle(prepared.title());
        rule.setAmount(prepared.amount());
        rule.setCurrency(prepared.currency());
        rule.setCategory(prepared.category());
        rule.setPayer(prepared.payer());
        rule.setSplitType(prepared.splitType());
        // A megadott értékeket tároljuk, az összegeket minden hónapban újraszámoljuk
        List<ExpenseShare> inputs = new ArrayList<>();
        prepared.shares().forEach(s -> inputs.add(new ExpenseShare(s.getPerson(), s.getInputValue(), s.getAmount())));
        rule.setShares(inputs);
        rule.setDayOfMonth(day);
        rule.setNextRunDate(LocalDate.now().plusMonths(1).withDayOfMonth(day));
        rule.setCreatedBy(currentUser.getName());
        rule.setCreatedAt(LocalDateTime.now());
        RecurringExpense saved = recurringRepository.save(rule);

        budgetService.createExpense(currentUser, groupId, input, saved.getId());
        return saved;
    }

    // Leállítás (törlés): a már létrejött kiadások megmaradnak.
    // A szabályt létrehozó tag, a tulajdonos vagy az admin teheti meg.
    @Transactional
    public void delete(User currentUser, Long groupId, Long recurringId) {
        GroupAccess access = groupService.requireAccess(currentUser, groupId);
        RecurringExpense rule = recurringRepository.findById(recurringId)
                .filter(r -> r.getGroup().getId().equals(groupId))
                .orElseThrow(() -> new NoSuchElementException("Nem található ismétlődő kiadás: " + recurringId));
        if (!access.canManage() && !rule.getCreatedBy().equals(currentUser.getName())) {
            throw new ForbiddenException("Csak a saját ismétlődő kiadásaidat állíthatod le!");
        }
        recurringRepository.delete(rule);
    }

    @Transactional
    public void deleteAllForGroup(Long groupId) {
        recurringRepository.deleteAll(recurringRepository.findAllByGroupIdOrderByNextRunDateAsc(groupId));
    }

    @Transactional
    public void deleteAll() {
        recurringRepository.deleteAll();
    }

    // Induláskor és utána óránként létrehozza az esedékes kiadásokat (a kimaradt hónapokat is pótolja).
    // Szándékosan nincs egy közös tranzakcióban: ha egy szabály hibás (pl. kilépett a fizető),
    // a többi szabály és a hibás szabály leállítása is mentődjön.
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 * * * *")
    public void createDueExpenses() {
        LocalDate today = LocalDate.now();
        for (RecurringExpense rule : recurringRepository.findAllByActiveTrueAndNextRunDateLessThanEqual(today)) {
            int created = 0;
            try {
                while (!rule.getNextRunDate().isAfter(today) && created < MAX_CATCH_UP_MONTHS) {
                    Expense expense = budgetService.createFromRecurring(rule, rule.getNextRunDate());
                    log.info("Ismétlődő kiadás létrehozva: {} ({})", expense.getTitle(), rule.getNextRunDate());
                    rule.setNextRunDate(rule.getNextRunDate().plusMonths(1).withDayOfMonth(rule.getDayOfMonth()));
                    created++;
                }
            } catch (IllegalArgumentException e) {
                // Pl. a fizető vagy egy résztvevő kilépett a csoportból: a szabályt leállítjuk
                log.warn("Ismétlődő kiadás leállítva ({}): {}", rule.getTitle(), e.getMessage());
                rule.setActive(false);
            }
            recurringRepository.save(rule);
        }
    }
}
