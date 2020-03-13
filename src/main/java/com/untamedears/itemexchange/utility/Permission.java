package com.untamedears.itemexchange.utility;

import com.google.common.base.Strings;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.util.NullCoalescing;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import static vg.civcraft.mc.namelayer.GroupManager.PlayerType;

/**
 * This class is an abstraction of NameLayer's Permission system. This is intended to bring together and bring ease of
 * use to NameLayer's somewhat awkward ways of doing things. For example, you are not returned your registered
 * permission upon registration, instead you must then ask for it later using the same information.
 */
public class Permission {

    private String slug;
    private Set<PlayerType> roles = Collections.emptySet();
    private String description;
    private boolean readonly;

    /**
     * Creates a Permission wrapper around an external permission. Please don't register this.
     *
     * @param slug The permission slug.
     * @throws IllegalArgumentException Throws if the Permission's slug is null or empty.
     */
    public Permission(String slug) {
        if (Strings.isNullOrEmpty(slug)) {
            throw new IllegalArgumentException("Cannot create that Permission: its slug is invalid.");
        }
        this.slug = slug;
        this.readonly = true;
    }

    /**
     * Creates a new Permission with a slug and default roles.
     *
     * @param slug The permission slug.
     * @param roles The default roles that will have this permission.
     * @throws IllegalArgumentException Throws if the Permission's slug is null or empty.
     */
    public Permission(String slug, Set<PlayerType> roles) {
        if (Strings.isNullOrEmpty(slug)) {
            throw new IllegalArgumentException("Cannot create that Permission: its slug is invalid.");
        }
        this.slug = slug;
        if (roles != null) {
            this.roles = roles;
        }
        this.readonly = false;
    }

    /**
     * Creates a new Permission with a slug, default roles, and a description.
     *
     * @param slug The permission slug.
     * @param roles The default roles that will have this permission. If none, just pass an empty set.
     * @param description The description of the permission.
     * @throws IllegalArgumentException Throws if the Permission's slug is null or empty. Or if the default roles set is
     *         null.
     */
    public Permission(String slug, Set<PlayerType> roles, String description) {
        this(slug, roles);
        this.description = description;
    }

    /**
     * Determines whether this Permission is just a wrapper for an external permission.
     *
     * @return Returns true if this Permission is only a wrapper.
     */
    public boolean isReadonly() {
        return this.readonly;
    }

    /**
     * Retrieves this permission from NameLayer.
     *
     * @return Returns the permission if it's currently registered, otherwise will return null.
     */
    public PermissionType getPermission() {
        return PermissionType.getPermission(this.slug);
    }

    /**
     * Attempts to register this Permission with NameLayer.
     */
    public void register() {
        if (this.readonly) {
            throw new UnsupportedOperationException("Cannot register that Permission: it's readonly.");
        }
        if (Strings.isNullOrEmpty(this.description)) {
            PermissionType.registerPermission(this.slug, new ArrayList<>(this.roles));
        }
        else {
            PermissionType.registerPermission(this.slug, new ArrayList<>(this.roles), this.description);
        }
    }

    /**
     * Checks whether a player has this Permission within a given group.
     *
     * @param group The group to check.
     * @param player The player to check.
     * @return Returns true if the player does have the permission within the given group.
     */
    public boolean hasAccess(Group group, Player player) {
        if (group == null || player == null) {
            return false;
        }
        PermissionType permission = getPermission();
        if (permission == null) {
            return false;
        }
        return NullCoalescing.chain(() ->
                NameAPI.getGroupManager().hasAccess(group, player.getUniqueId(), permission), false);
    }

    /**
     * @return Returns a set of MEMBERS, MODS, ADMINS, OWNER
     */
    public static Set<PlayerType> membersAndAbove() {
        return Collections.unmodifiableSet(new TreeSet<PlayerType>() {{
            add(PlayerType.MEMBERS);
            addAll(modsAndAbove());
        }});
    }

    /**
     * @return Returns a set of MODS, ADMINS, OWNER
     */
    public static Set<PlayerType> modsAndAbove() {
        return Collections.unmodifiableSet(new TreeSet<PlayerType>() {{
            add(PlayerType.MODS);
            addAll(adminsAndAbove());
        }});
    }

    /**
     * @return Returns a set of ADMINS, OWNER
     */
    public static Set<PlayerType> adminsAndAbove() {
        return Collections.unmodifiableSet(new TreeSet<PlayerType>() {{
            add(PlayerType.ADMINS);
            add(PlayerType.OWNER);
        }});
    }

}
