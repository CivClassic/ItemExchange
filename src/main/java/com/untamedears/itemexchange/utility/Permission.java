package com.untamedears.itemexchange.utility;

import com.google.common.base.Strings;
import org.bukkit.entity.Player;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import static vg.civcraft.mc.namelayer.GroupManager.PlayerType;

public class Permission {

    private String name;
    private Set<PlayerType> roles;
    private String description;

    public Permission(String name, Set<PlayerType> roles) {
        this.name = name;
        this.roles = roles;
    }

    public Permission(String name, Set<PlayerType> roles, String description) {
        this(name, roles);
        this.description = description;
    }

    public PermissionType getPermission() {
        return PermissionType.getPermission(this.name);
    }

    public void register() {
        if (Strings.isNullOrEmpty(this.description)) {
            PermissionType.registerPermission(this.name, new ArrayList<>(this.roles));
        }
        else {
            PermissionType.registerPermission(this.name, new ArrayList<>(this.roles), this.description);
        }
    }

    public boolean hasAccess(Group group, Player player) {
        if (group == null || player == null) {
            return false;
        }
        PermissionType permission = getPermission();
        if (permission == null) {
            return false;
        }
        return NameAPI.getGroupManager().hasAccess(group, player.getUniqueId(), permission);
    }

    public static Set<PlayerType> membersAndAbove() {
        return Collections.unmodifiableSet(new TreeSet<PlayerType>() {{
            add(PlayerType.MEMBERS);
            addAll(modsAndAbove());
        }});
    }

    public static Set<PlayerType> modsAndAbove() {
        return Collections.unmodifiableSet(new TreeSet<PlayerType>() {{
            add(PlayerType.MODS);
            addAll(adminsAndAbove());
        }});
    }

    public static Set<PlayerType> adminsAndAbove() {
        return Collections.unmodifiableSet(new TreeSet<PlayerType>() {{
            add(PlayerType.ADMINS);
            add(PlayerType.OWNER);
        }});
    }

}
