package hu.unideb.inf.szoftverfejlesztes.config;

import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.model.ExpenseGroup;
import hu.unideb.inf.szoftverfejlesztes.model.GroupMember;
import hu.unideb.inf.szoftverfejlesztes.model.Settlement;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseGroupRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.GroupMemberRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.SettlementRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.UserRepository;
import hu.unideb.inf.szoftverfejlesztes.services.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Indításkor: ha még egyetlen csoport sincs, létrehozza az alapcsoportot ("KözösKassza csapat") az összes
// (nem admin) felhasználóval, Zoli a tulajdonos. A csoportok bevezetése előtti kiadások és rendezések
// (amelyeknek nincs csoportjuk) a legelső csoportba kerülnek.
@Component
@Order(2) // a felhasználók (DefaultUsersInitializer) után, a demó adatok (DemoDataInitializer) előtt
public class DefaultGroupInitializer implements ApplicationRunner {

    public static final String DEFAULT_GROUP_NAME = "KözösKassza csapat";
    private static final String DEFAULT_OWNER = "Zoli";

    private static final Logger log = LoggerFactory.getLogger(DefaultGroupInitializer.class);

    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final GroupService groupService;

    public DefaultGroupInitializer(ExpenseGroupRepository groupRepository, GroupMemberRepository memberRepository,
                                   UserRepository userRepository, ExpenseRepository expenseRepository,
                                   SettlementRepository settlementRepository, GroupService groupService) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.groupService = groupService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (groupRepository.count() == 0) {
            createDefaultGroup();
        }
        assignOrphanData();
    }

    private void createDefaultGroup() {
        List<User> users = userRepository.findAllByAdminFalse();
        if (users.isEmpty()) {
            return;
        }
        User owner = users.stream().filter(u -> u.getName().equals(DEFAULT_OWNER)).findFirst().orElse(users.get(0));

        ExpenseGroup group = groupRepository.save(new ExpenseGroup(DEFAULT_GROUP_NAME,
                "Lakótársak közös kiadásai", groupService.generateInviteCode(), owner.getName()));
        for (User user : users) {
            GroupMember.Role role = user.getId().equals(owner.getId()) ? GroupMember.Role.OWNER : GroupMember.Role.MEMBER;
            memberRepository.save(new GroupMember(group, user, role));
        }
        log.info("Alapcsoport létrehozva: {} ({} taggal)", DEFAULT_GROUP_NAME, users.size());
    }

    // A csoportok bevezetése előtti adatok a legelső csoportba kerülnek
    private void assignOrphanData() {
        List<Expense> orphanExpenses = expenseRepository.findAllByGroupIsNull();
        List<Settlement> orphanSettlements = settlementRepository.findAllByGroupIsNull();
        if (orphanExpenses.isEmpty() && orphanSettlements.isEmpty()) {
            return;
        }
        groupRepository.findAll(Sort.by("id")).stream().findFirst().ifPresent(group -> {
            orphanExpenses.forEach(e -> e.setGroup(group));
            orphanSettlements.forEach(s -> s.setGroup(group));
            expenseRepository.saveAll(orphanExpenses);
            settlementRepository.saveAll(orphanSettlements);
            log.info("Csoport nélküli adatok áthelyezve ide: {} ({} kiadás, {} rendezés)",
                    group.getName(), orphanExpenses.size(), orphanSettlements.size());
        });
    }
}
