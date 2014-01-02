package test;

public class Failure {
  public void method(int a) {
    // Copied from https://code.google.com/p/error-prone/wiki/DivZero
    double y = (double) a / 0.0
  }
}
