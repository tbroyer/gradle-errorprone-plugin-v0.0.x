package test;

public class Failure {
  // See http://errorprone.info/bugpattern/ArrayEquals
  public boolean arrayEquals(int[] a, int[] b) {
    return a.equals(b);
  }
}
