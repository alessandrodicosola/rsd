package recommendations.skel

/**
 * @param ValueType Type value of the ratings inside the dataset
 */
interface ITestable<ValueType> {
    fun test() : ValueType
}