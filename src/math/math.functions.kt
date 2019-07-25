package math

fun sigmoid(value: Double) : Double {
    return 1/(1+Math.pow(Math.E,value));
}

fun derivateSigmoid(value: Double) : Double {
    return sigmoid(value)*(1- sigmoid(value));
}

