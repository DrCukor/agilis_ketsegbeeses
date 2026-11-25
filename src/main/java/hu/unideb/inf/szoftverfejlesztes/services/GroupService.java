package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.ExpenseGroup;
import hu.unideb.inf.szoftverfejlesztes.model.GroupMember;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseGroupRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

// Csoportok és tagságok kezelése, valamint a csoportszintű jogosultság-ellenőrzés
@Service
public class GroupService {

    // Félreérthető karakterek (0/O, 1/I) nélkül
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_NAME_LENGTH = 60;
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final SecureRandom random = new SecureRandom();

    public GroupService(ExpenseGroupRepository groupRepository, GroupMemberRepository memberRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
    }

    // A bejelentkezett felhasználó hozzáférése egy csoporthoz.
    // Tag: látja és használja; tulajdonos / rendszer-admin: kezeli is.
    public record GroupAccess(ExpenseGroup group, User user, GroupMember.Role role, boolean systemAdmin) {
        public boolean isMember() {
            return role != null;
        }

        public boolean canManage() {
            return systemAdmin || role == GroupMember.Role.OWNER;
        }

        // A felületen megjelenő szerep: OWNER / MEMBER / ADMIN
        public String roleName() {
            return role != null ? role.name() : "ADMIN";
        }
    }

    // Hozzáférés ellenőrzése: nem létező csoportnál 404, ha nem tag (és nem admin) 403
    public GroupAccess requireAccess(User user, Long groupId) {
        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Nem található csoport: " + groupId));
        Optional<GroupMember> membership = memberRepository.findByGroupIdAndUserId(groupId, user.getId());
        if (membership.isEmpty() && !user.isAdmin()) {
            throw new ForbiddenException("Nem vagy tagja ennek a csoportnak!");
        }
        return new GroupAccess(group, user, membership.map(GroupMember::getRole).orElse(null), user.isAdmin());
    }

    // A felhasználó csoportjai (a rendszer-admin mindet látja)
    public List<GroupAccess> getGroupsFor(User user) {
        if (user.isAdmin()) {
            return groupRepository.findAllByOrderByNameAsc().stream()
                    .map(group -> new GroupAccess(group, user,
                            memberRepository.findByGroupIdAndUserId(group.getId(), user.getId())
                                    .map(GroupMember::getRole).orElse(null), true))
                    .toList();
        }
        Collator hungarian = hungarianCollator();
        return memberRepository.findAllByUserId(user.getId()).stream()
                .map(m -> new GroupAccess(m.getGroup(), user, m.getRole(), false))
                .sorted(Comparator.comparing((GroupAccess a) -> a.group().getName(), hungarian))
                .toList();
    }

    // A csoport tagjai, magyar ábécérendben
    public List<GroupMember> getMembers(Long groupId) {
        Collator hungarian = hungarianCollator();
        return memberRepository.findAllByGroupId(groupId).stream()
                .sorted(Comparator.comparing((GroupMember m) -> m.getUser().getName(), hungarian))
                .toList();
    }

    // A csoport tagjainak nevei (ők szerepelhetnek fizetőként / résztvevőként)
    public List<String> getMemberNames(Long groupId) {
        return getMembers(groupId).stream().map(m -> m.getUser().getName()).toList();
    }

    @Transactional
    public ExpenseGroup createGroup(User user, String name, String description) {
        if (user.isAdmin()) {
            throw new ForbiddenException("Az admin nem hozhat létre csoportot, mert nem vesz részt a kiadásokban.");
        }
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("A csoport neve nem lehet üres!");
        }
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("A csoport neve legfeljebb " + MAX_NAME_LENGTH + " karakter lehet!");
        }
        String trimmedDescription = description == null || description.isBlank() ? null : description.trim();
        if (trimmedDescription != null && trimmedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("A leírás legfeljebb " + MAX_DESCRIPTION_LENGTH + " karakter lehet!");
        }

        ExpenseGroup group = groupRepository.save(
                new ExpenseGroup(trimmedName, trimmedDescription, generateInviteCode(), user.getName()));
        memberRepository.save(new GroupMember(group, user, GroupMember.Role.OWNER));
        return group;
    }

    // Csoport keresése meghívókód alapján (a csatlakozás előtti előnézethez is)
    public ExpenseGroup findByInviteCode(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return groupRepository.findByInviteCode(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Érvénytelen meghívókód!"));
    }

    public boolean isMember(Long groupId, User user) {
        return memberRepository.findByGroupIdAndUserId(groupId, user.getId()).isPresent();
    }

    @Transactional
    public ExpenseGroup joinByInviteCode(User user, String code) {
        if (user.isAdmin()) {
            throw new ForbiddenException("Az admin nem csatlakozhat csoporthoz, mert nem vesz részt a kiadásokban.");
        }
        ExpenseGroup group = findByInviteCode(code);
        if (isMember(group.getId(), user)) {
            throw new IllegalArgumentException("Már tagja vagy ennek a csoportnak!");
        }
        memberRepository.save(new GroupMember(group, user, GroupMember.Role.MEMBER));
        return group;
    }

    // Tulajdonjog átadása egy másik tagnak (csak a tulajdonos vagy az admin teheti meg)
    @Transactional
    public void transferOwnership(User user, Long groupId, String newOwnerName) {
        GroupAccess access = requireAccess(user, groupId);
        if (!access.canManage()) {
            throw new ForbiddenException("Csak a csoport tulajdonosa adhatja át a tulajdonjogot!");
        }
        List<GroupMember> members = memberRepository.findAllByGroupId(groupId);
        GroupMember newOwner = members.stream()
                .filter(m -> m.getUser().getName().equals(newOwnerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nem tagja a csoportnak: " + newOwnerName));
        if (newOwner.getRole() == GroupMember.Role.OWNER) {
            throw new IllegalArgumentException(newOwnerName + " már most is a tulajdonos!");
        }
        members.stream()
                .filter(m -> m.getRole() == GroupMember.Role.OWNER)
                .forEach(m -> m.setRole(GroupMember.Role.MEMBER));
        newOwner.setRole(GroupMember.Role.OWNER);
        memberRepository.saveAll(members);
    }

    // Egyedi, könnyen begépelhető meghívókód, pl. "K7F3-QM2P"
    public String generateInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                if (i == 4) {
                    sb.append('-');
                }
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (groupRepository.existsByInviteCode(code));
        return code;
    }

    private static Collator hungarianCollator() {
        return Collator.getInstance(Locale.forLanguageTag("hu-HU"));
    }
}
