package recommendations.skel

/**
 * Interfaccia che identifica sistemi di raccomandazione testabili
 * @param ValueType Type value of the ratings inside the dataset
 */
interface ITestable<ValueType> {
    fun test(): ValueType
}