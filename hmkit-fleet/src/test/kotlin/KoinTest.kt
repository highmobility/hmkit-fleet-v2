//import io.mockk.verify
import org.junit.experimental.categories.Category
import org.junit.jupiter.api.Test
import org.koin.test.category.CheckModuleTest
import org.koin.test.check.checkModules

@Category(CheckModuleTest::class)
class ModuleCheckTest : BaseTest() {

    @Test
    fun checkModuless() {
        checkModules {
            modules(modules)
        }
    }
}
