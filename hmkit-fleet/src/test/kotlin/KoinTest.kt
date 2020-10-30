//import io.mockk.verify
import io.mockk.verify
import network.WebService
import org.junit.experimental.categories.Category
import org.junit.jupiter.api.Test
import org.koin.test.AutoCloseKoinTest
import org.koin.test.category.CheckModuleTest
import org.koin.test.check.checkModules
import org.koin.test.get

@Category(CheckModuleTest::class)
class ModuleCheckTest : BaseTest() {

    @Test
    fun checkModuless() {
        checkModules {
            modules(modules)
        }
    }
}
