package com.fekete.bangdemo.data

import kotlinx.serialization.Serializable

/**
 * Class responsible for converting the integer [classId] into the string [linkId]. Mostly used by the
 * [com.fekete.bangdemo.MyApp.class2linkService].
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
@Serializable
data class Class2Link(
    val classId: Int,
    val linkId: String
)