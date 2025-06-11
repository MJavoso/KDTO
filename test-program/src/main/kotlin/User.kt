import com.marcode.kdto.annotations.Dto
import com.marcode.kdto.annotations.DtoSpec
import java.time.LocalDate

@Dto(
    dtoSpec = [
        DtoSpec("UserDTO", exclude = ["id"])
    ]
)
@MyAnnotation(metadata = "Hello")
data class User(
    val id: Int? = null,
    val name: String,
    val birthDate: LocalDate
)