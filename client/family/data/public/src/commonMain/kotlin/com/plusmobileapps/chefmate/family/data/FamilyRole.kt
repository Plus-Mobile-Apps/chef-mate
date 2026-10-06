package com.plusmobileapps.chefmate.family.data

/** A person's permission level within a family. See [FamilyPermissions] for what each may do. */
enum class FamilyRole {
    /** Created the family. Can't be removed and is the only one who can manage admins. */
    OWNER,
    /** Can invite and remove members and rename the family. */
    ADMIN,
    /** Can view the family and leave it. */
    MEMBER;

    /** The wire value used by Supabase's `family_role` enum. */
    val wireValue: String
        get() = name.lowercase()

    companion object {
        fun fromWire(value: String?): FamilyRole =
            entries.firstOrNull { it.wireValue == value?.lowercase() } ?: MEMBER
    }
}
