package hu.unideb.inf.szoftverfejlesztes.controller;

import hu.unideb.inf.szoftverfejlesztes.config.SessionManager;
import hu.unideb.inf.szoftverfejlesztes.model.Currency;
import hu.unideb.inf.szoftverfejlesztes.model.ExpenseGroup;
import hu.unideb.inf.szoftverfejlesztes.model.GroupMember;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.services.BudgetService;
import hu.unideb.inf.szoftverfejlesztes.services.GroupService;
import hu.unideb.inf.szoftverfejlesztes.services.GroupService.GroupAccess;
import hu.unideb.inf.szoftverfejlesztes.services.MembershipService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Csoportok: listázás, létrehozás, csatlakozás meghívókóddal, kilépés, tulajdonjog átadása, törlés.
// A hibák (400 / 403 / 404) kezelése a GlobalExceptionHandler-ben történik.
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final MembershipService membershipService;
    private final BudgetService budgetService;

    public GroupController(GroupService groupService, MembershipService membershipService, BudgetService budgetService) {
        this.groupService = groupService;
        this.membershipService = membershipService;
        this.budgetService = budgetService;
    }

    // A felhasználó csoportjai, mindegyiknél a saját egyenlegével (az admin mindet látja)
    @GetMapping
    public List<GroupSummary> getMyGroups(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser) {
        return groupService.getGroupsFor(currentUser).stream()
                .map(access -> toSummary(access, currentUser))
                .toList();
    }

    @PostMapping
    public ResponseEntity<GroupSummary> createGroup(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                                    @RequestBody CreateGroupRequest request) {
        ExpenseGroup group = groupService.createGroup(currentUser, request.getName(), request.getDescription());
        GroupAccess access = groupService.requireAccess(currentUser, group.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toSummary(access, currentUser));
    }

    // Meghívó előnézete (a csatlakozás megerősítéséhez): név, leírás, létszám
    @GetMapping("/invite/{code}")
    public InvitePreview previewInvite(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                       @PathVariable String code) {
        ExpenseGroup group = groupService.findByInviteCode(code);
        return new InvitePreview(group.getId(), group.getName(), group.getDescription(),
                groupService.getMembers(group.getId()).size(), groupService.isMember(group.getId(), currentUser));
    }

    @PostMapping("/join")
    public GroupSummary joinGroup(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                  @RequestBody JoinRequest request) {
        ExpenseGroup group = groupService.joinByInviteCode(currentUser, request.getCode());
        return toSummary(groupService.requireAccess(currentUser, group.getId()), currentUser);
    }

    // Csoport részletei: tagok szerepekkel, meghívókód, saját szerep
    @GetMapping("/{groupId}")
    public GroupDetails getGroup(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                 @PathVariable Long groupId) {
        GroupAccess access = groupService.requireAccess(currentUser, groupId);
        ExpenseGroup group = access.group();
        List<MemberInfo> members = groupService.getMembers(groupId).stream()
                .map(m -> new MemberInfo(m.getUser().getName(), m.getRole().name(), m.getJoinedAt()))
                .toList();
        return new GroupDetails(group.getId(), group.getName(), group.getDescription(), group.getInviteCode(),
                access.roleName(), access.canManage(), members);
    }

    // Kilépés; ha a tulajdonos volt az utolsó tag, a csoport is törlődik
    @PostMapping("/{groupId}/leave")
    public Map<String, Boolean> leaveGroup(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                           @PathVariable Long groupId) {
        boolean groupDeleted = membershipService.leave(currentUser, groupId);
        return Map.of("groupDeleted", groupDeleted);
    }

    @PostMapping("/{groupId}/transfer")
    public ResponseEntity<Void> transferOwnership(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                                  @PathVariable Long groupId, @RequestBody TransferRequest request) {
        groupService.transferOwnership(currentUser, groupId, request.getNewOwner());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                            @PathVariable Long groupId) {
        membershipService.deleteGroup(currentUser, groupId);
        return ResponseEntity.noContent().build();
    }

    private GroupSummary toSummary(GroupAccess access, User currentUser) {
        ExpenseGroup group = access.group();
        List<Map<Currency, Double>> balance = budgetService.getPersonalBalance(group.getId(), currentUser.getName());
        return new GroupSummary(group.getId(), group.getName(), group.getDescription(), group.getInviteCode(),
                groupService.getMembers(group.getId()).size(), access.roleName(), access.canManage(),
                balance.get(0), balance.get(1));
    }

    // owe: a felhasználó tartozása pénznemenként, owed: ami neki jár
    public record GroupSummary(Long id, String name, String description, String inviteCode, int memberCount,
                               String role, boolean canManage, Map<Currency, Double> owe, Map<Currency, Double> owed) {
    }

    public record GroupDetails(Long id, String name, String description, String inviteCode, String role,
                               boolean canManage, List<MemberInfo> members) {
    }

    public record MemberInfo(String name, String role, LocalDateTime joinedAt) {
    }

    public record InvitePreview(Long id, String name, String description, int memberCount, boolean alreadyMember) {
    }

    @Data
    public static class CreateGroupRequest {
        private String name;
        private String description;
    }

    @Data
    public static class JoinRequest {
        private String code;
    }

    @Data
    public static class TransferRequest {
        private String newOwner;
    }
}
