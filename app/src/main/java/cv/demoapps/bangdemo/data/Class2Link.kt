package cv.demoapps.bangdemo.data

import kotlinx.serialization.Serializable

@Serializable
data class Class2Link(
    val classId: Int,
    val linkId: String
)