import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals

class PassByValueByReferenceTest {


    @Test
    fun testDoubleArray() {
        val random = Random(System.currentTimeMillis())
        var testArray = DoubleArray(10, { random.nextDouble() })

        method1(testArray, 1, 10.0);
        assertEquals(10.0, testArray[1])
    }

    @Test
    fun testMapDoubleArray() {
        val random = Random(System.currentTimeMillis())
        val map = listOf(11, 22, 33, 44, 55).associate { it to DoubleArray(10, { random.nextDouble() }) }
        method2(map, 33, 2, 100.0)
        assertEquals(100.0, map[33]!![2])
    }

    private fun method1(array: DoubleArray, index: Int, value: Double) {
        array[index] = value
    }

    private fun method2(map: Map<Int, DoubleArray>, key: Int, index: Int, value: Double) {
        map[key]!![index] = value
    }
}