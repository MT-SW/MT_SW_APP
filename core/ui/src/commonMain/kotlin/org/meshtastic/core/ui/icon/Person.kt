/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.core.ui.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ic_account_circle
import org.meshtastic.core.resources.ic_group
import org.meshtastic.core.resources.ic_contacts_remote
import org.meshtastic.core.resources.ic_groups
import org.meshtastic.core.resources.ic_person
import org.meshtastic.core.resources.ic_person_add
import org.meshtastic.core.resources.ic_person_off
import org.meshtastic.core.resources.ic_person_question
import org.meshtastic.core.resources.ic_person_remove
import org.meshtastic.core.resources.ic_person_search

val MeshtasticIcons.PersonOff: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_person_off)
val MeshtasticIcons.PersonQuestion: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_person_question)
val MeshtasticIcons.Group: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_group)
val MeshtasticIcons.AccountCircle: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_account_circle)
val MeshtasticIcons.PersonSearch: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_person_search)

val MeshtasticIcons.PersonAdd: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_person_add)
/** MT_SW: a person with a minus, for removing a contact. */
val MeshtasticIcons.PersonRemove: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_person_remove)
val MeshtasticIcons.Person: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_person)
/** MT_SW: the address book with the person and the signal, for the remote lists and contacts of a device. */
val MeshtasticIcons.RemoteContacts: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_contacts_remote)
val MeshtasticIcons.Groups: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_groups)
val MeshtasticIcons.PeopleCount: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_group)
