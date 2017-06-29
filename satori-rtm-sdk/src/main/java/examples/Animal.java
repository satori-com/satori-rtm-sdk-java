package examples;

import java.util.*;

/**
 * Sample model to publish to RTM.
 *
 * This class represents the following raw json structure:
 * <pre>{@literal
 * {
 *   "who": "zebra",
 *   "where": [34.134358, -118.321506]
 * }
 * }</pre>
 */
class Animal {
  String who;
  float[] where;

  Animal() {}

  Animal(String who, float[] where) {
    this.who = who;
    this.where = where;
  }

  @Override
  public String toString() {
    return "Animal{" +
        "who='" + who + '\'' +
        ", where=" + Arrays.toString(where) +
        '}';
  }
}
