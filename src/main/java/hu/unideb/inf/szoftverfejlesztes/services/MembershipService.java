package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.GroupMember;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseGroupRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.GroupMemberRepository;
import hu.unideb.inf.szoftverfejlesztes.services.GroupService.GroupAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Kilépés csoportból és csoport törlése: ezekhez a tagság mellett az egyenlegeket és az
// ismétlődő kiadásokat is figyelembe kell venni
@Service
public class MembershipService {

    private final GroupService groupService;
    private final BudgetService budgetService;
    private final RecurringExpenseService recurringExpenseService;
    private final GroupMemberRepository memberRepository;
    private final ExpenseGroupRepository groupRepository;

    public MembershipService(GroupService groupService, BudgetService budgetService,
                             RecurringExpenseService recurringExpenseService,
                             GroupMemberRepository memberRepository, ExpenseGroupRepository groupRepository) {
        this.groupService = groupService;
        this.budgetService = budgetService;
        this.recurringExpenseService = recurringExpenseService;
        this.memberRepository = memberRepository;
        this.groupRepository = groupRepository;
    }

    // Kilépés: csak nyitott tartozás / követelés nélkül. A tulajdonos csak akkor léphet ki,
    // ha ő az utolsó tag (ilyenkor a csoport törlődik) – különben előbb át kell adnia a tulajdonjogot.
    // Visszatérési érték: true, ha a csoport is törlődött.
    @Transactional
    public boolean leave(User currentUser, Long groupId) {
        GroupAccess access = groupService.requireAccess(currentUser, groupId);
        if (!access.isMember()) {
            throw new IllegalArgumentException("Nem vagy tagja ennek a csoportnak!");
        }
        if (budgetService.hasOpenBalance(groupId, currentUser.getName())) {
            throw new IllegalArgumentException("Nem léphetsz ki, amíg van nyitott tartozásod vagy követelésed a csoportban!");
        }

        List<GroupMember> members = memberRepository.findAllByGroupId(groupId);
        if (access.role() == GroupMember.Role.OWNER) {
            if (members.size() > 1) {
                throw new IllegalArgumentException("Tulajdonosként előbb add át a tulajdonjogot egy másik tagnak!");
            }
            deleteGroupInternal(groupId);
            return true;
        }

        members.stream()
                .filter(m -> m.getUser().getId().equals(currentUser.getId()))
                .forEach(memberRepository::delete);
        return false;
    }

    // Csoport végleges törlése minden adatával együtt (tulajdonos / admin)
    @Transactional
    public void deleteGroup(User currentUser, Long groupId) {
        GroupAccess access = groupService.requireAccess(currentUser, groupId);
        if (!access.canManage()) {
            throw new ForbiddenException("A csoportot csak a tulajdonos törölheti!");
        }
        deleteGroupInternal(groupId);
    }

    private void deleteGroupInternal(Long groupId) {
        recurringExpenseService.deleteAllForGroup(groupId);
        budgetService.deleteGroupDataInternal(groupId);
        memberRepository.deleteAllByGroupId(groupId);
        groupRepository.deleteById(groupId);
    }
}
