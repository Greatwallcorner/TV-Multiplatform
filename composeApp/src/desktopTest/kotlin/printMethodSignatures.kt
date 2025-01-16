import com.github.catvod.net.OkHttp
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaMethod


class SignatureTest{

    fun printMethodSignatures(clazz: KClass<*>) {
        clazz.declaredFunctions.forEach { function ->
            println(function.javaMethod.toString())
        }
    }

    @Test
    fun test() {
        printMethodSignatures(OkHttp::class)
    }
}
