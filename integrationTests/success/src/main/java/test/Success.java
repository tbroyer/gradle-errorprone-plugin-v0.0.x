package test;

public class Success {
  // See http://errorprone.info/bugpattern/ArrayEquals
  @SuppressWarnings("ArrayEquals")
  public boolean arrayEquals(int[] a, int[] b) {
    return a.equals(b);
  }
}
